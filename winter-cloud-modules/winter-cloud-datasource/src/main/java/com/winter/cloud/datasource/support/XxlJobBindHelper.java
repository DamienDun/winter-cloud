package com.winter.cloud.datasource.support;

import com.alibaba.fastjson2.JSON;
import com.winter.cloud.common.core.constant.HttpStatus;
import com.winter.cloud.common.core.constant.SecurityConstants;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.security.utils.SecurityUtils;
import com.winter.cloud.datasource.constant.DsJobConstants;
import com.winter.cloud.datasource.domain.entity.DsJobInfo;
import com.winter.cloud.xxljob.api.RemoteXxlJobGroupService;
import com.winter.cloud.xxljob.api.RemoteXxlJobInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bind/sync XXL-JOB via Feign (executor appname=datasource-data-extra)
 */
@Component
public class XxlJobBindHelper {

    @Autowired
    private RemoteXxlJobInfoService remoteXxlJobInfoService;

    @Autowired
    private RemoteXxlJobGroupService remoteXxlJobGroupService;

    public Long createXxlJob(DsJobInfo job) {
        Map<String, Object> body = buildXxlJobBody(null, job);
        AjaxResult result = remoteXxlJobInfoService.addInner(body, SecurityConstants.INNER);
        assertOk(result, "create xxl job");
        Object data = result.get(AjaxResult.DATA_TAG);
        if (data == null) {
            throw new IllegalStateException("xxl job created but id missing");
        }
        return Long.valueOf(String.valueOf(data));
    }

    public void updateXxlJob(DsJobInfo job) {
        if (job.getXxlJobId() == null) {
            throw new IllegalStateException("job not bound to xxl-job");
        }
        Map<String, Object> body = buildXxlJobBody(job.getXxlJobId().intValue(), job);
        assertOk(remoteXxlJobInfoService.editInner(body, SecurityConstants.INNER), "update xxl job");
    }

    public void removeXxlJob(Long xxlJobId) {
        if (xxlJobId == null) {
            return;
        }
        assertOk(remoteXxlJobInfoService.removeInner(xxlJobId.intValue(), SecurityConstants.INNER), "remove xxl job");
    }

    public void startXxlJob(Long xxlJobId) {
        assertOk(remoteXxlJobInfoService.startInner(xxlJobId.intValue(), SecurityConstants.INNER), "start xxl job");
    }

    public void stopXxlJob(Long xxlJobId) {
        assertOk(remoteXxlJobInfoService.stopInner(xxlJobId.intValue(), SecurityConstants.INNER), "stop xxl job");
    }

    public void triggerXxlJob(Long xxlJobId, String executorParam) {
        Map<String, Object> body = new HashMap<>(4);
        body.put("id", xxlJobId.intValue());
        body.put("executorParam", executorParam == null ? "" : executorParam);
        body.put("addressList", "");
        assertOk(remoteXxlJobInfoService.triggerInner(body, SecurityConstants.INNER), "trigger xxl job");
    }

    @SuppressWarnings("unchecked")
    public List<String> nextTriggerTime(String cron) {
        AjaxResult result = remoteXxlJobInfoService.nextTriggerTimeInner("CRON", cron, SecurityConstants.INNER);
        assertOk(result, "nextTriggerTime");
        Object data = result.get(AjaxResult.DATA_TAG);
        if (data instanceof List) {
            return (List<String>) data;
        }
        return JSON.parseArray(JSON.toJSONString(data), String.class);
    }

    private Map<String, Object> buildXxlJobBody(Integer xxlJobId, DsJobInfo job) {
        Map<String, Object> body = new HashMap<>(24);
        if (xxlJobId != null) {
            body.put("id", xxlJobId);
        }
        body.put("jobGroup", resolveJobGroupId());
        body.put("jobDesc", job.getJobName());
        String author = SecurityUtils.getUsername();
        body.put("author", StringUtils.isEmpty(author) ? "datasource" : author);
        body.put("alarmEmail", "");
        if (Integer.valueOf(DsJobConstants.TRIGGER_CRON).equals(job.getTriggerType())) {
            body.put("scheduleType", "CRON");
            body.put("scheduleConf", job.getCron() == null ? "" : job.getCron());
        } else {
            body.put("scheduleType", "NONE");
            body.put("scheduleConf", "");
        }
        body.put("glueType", "BEAN");
        body.put("executorHandler", DsJobConstants.EXECUTOR_HANDLER);
        body.put("executorParam", "");
        body.put("executorRouteStrategy", "ROUND");
        body.put("misfireStrategy", "DO_NOTHING");
        body.put("executorBlockStrategy", "SERIAL_EXECUTION");
        body.put("executorTimeout", 0);
        body.put("executorFailRetryCount", 0);
        body.put("glueRemark", "GLUE code init");
        body.put("glueSource", "");
        body.put("childJobId", "");
        return body;
    }

    @SuppressWarnings("unchecked")
    private int resolveJobGroupId() {
        AjaxResult all = remoteXxlJobGroupService.allInner(SecurityConstants.INNER);
        assertOk(all, "list job group");
        Object data = all.get(AjaxResult.DATA_TAG);
        List<Map<String, Object>> groups = JSON.parseObject(JSON.toJSONString(data), List.class);
        if (groups != null) {
            for (Object item : groups) {
                Map<String, Object> g = item instanceof Map ? (Map<String, Object>) item : JSON.parseObject(JSON.toJSONString(item), Map.class);
                if (DsJobConstants.EXECUTOR_APP_NAME.equals(String.valueOf(g.get("appname")))) {
                    return Integer.parseInt(String.valueOf(g.get("id")));
                }
            }
        }
        Map<String, Object> create = new HashMap<>(8);
        create.put("appname", DsJobConstants.EXECUTOR_APP_NAME);
        create.put("title", DsJobConstants.EXECUTOR_TITLE);
        // 0=自动注册；勿改成手动地址，端口写错会导致触发失败、作业日志无数据
        create.put("addressType", 0);
        create.put("addressList", "");
        assertOk(remoteXxlJobGroupService.addInner(create, SecurityConstants.INNER), "create job group");

        AjaxResult again = remoteXxlJobGroupService.allInner(SecurityConstants.INNER);
        assertOk(again, "relist job group");
        Object againData = again.get(AjaxResult.DATA_TAG);
        List<Map<String, Object>> againGroups = JSON.parseObject(JSON.toJSONString(againData), List.class);
        if (againGroups != null) {
            for (Object item : againGroups) {
                Map<String, Object> g = item instanceof Map ? (Map<String, Object>) item : JSON.parseObject(JSON.toJSONString(item), Map.class);
                if (DsJobConstants.EXECUTOR_APP_NAME.equals(String.valueOf(g.get("appname")))) {
                    return Integer.parseInt(String.valueOf(g.get("id")));
                }
            }
        }
        throw new IllegalStateException("executor not found: " + DsJobConstants.EXECUTOR_APP_NAME);
    }

    private void assertOk(AjaxResult result, String action) {
        if (result == null) {
            throw new IllegalStateException(action + " failed: empty response");
        }
        Object code = result.get(AjaxResult.CODE_TAG);
        int c = code == null ? HttpStatus.ERROR : Integer.parseInt(String.valueOf(code));
        if (c != HttpStatus.SUCCESS) {
            Object msg = result.get(AjaxResult.MSG_TAG);
            throw new IllegalStateException(action + " failed: " + (msg == null ? "unknown" : msg));
        }
    }
}
