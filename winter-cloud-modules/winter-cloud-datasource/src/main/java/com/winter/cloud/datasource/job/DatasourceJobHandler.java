package com.winter.cloud.datasource.job;

import com.alibaba.fastjson2.JSON;
import com.winter.cloud.datasource.constant.DsJobConstants;
import com.winter.cloud.datasource.domain.dto.DsJobExecuteParam;
import com.winter.cloud.datasource.domain.entity.DsJobInfo;
import com.winter.cloud.datasource.domain.entity.DsJobLog;
import com.winter.cloud.datasource.service.IDsJobInfoService;
import com.winter.cloud.datasource.service.IDsJobLogService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Datasource job handler: write job log and patch last execute status.
 * Sync business logic can be extended later; current skeleton closes the schedule loop.
 */
@Component
public class DatasourceJobHandler {

    private static final Logger log = LoggerFactory.getLogger(DatasourceJobHandler.class);

    @Autowired
    private IDsJobInfoService jobInfoService;

    @Autowired
    private IDsJobLogService jobLogService;

    @XxlJob(DsJobConstants.EXECUTOR_HANDLER)
    public void datasourceJobHandler() {
        String paramStr = XxlJobHelper.getJobParam();
        long xxlJobId = XxlJobHelper.getJobId();
        log.info("datasourceJobHandler start, xxlJobId={}, param={}", xxlJobId, paramStr);

        DsJobExecuteParam param = parseParam(paramStr);
        DsJobInfo job = resolveJob(param, xxlJobId);
        if (job == null) {
            XxlJobHelper.handleFail("bound job not found, xxlJobId=" + xxlJobId);
            return;
        }

        Date begin = new Date();
        DsJobLog jobLog = new DsJobLog();
        jobLog.setJobId(job.getId());
        jobLog.setJobName(job.getJobName());
        jobLog.setTriggerType(param != null && param.getTriggerType() != null
                ? param.getTriggerType() : DsJobConstants.TRIGGER_CRON);
        jobLog.setTriggerUserId(param != null ? param.getTriggerUserId() : null);
        jobLog.setTriggerUsername(param != null ? param.getTriggerUsername() : "xxl-job");
        jobLog.setExecuteStatus(DsJobConstants.LOG_RUNNING);
        jobLog.setExecuteBeginTime(begin);
        jobLog.setDatasourceType(job.getDatasourceType());
        jobLog.setJobMessage("job started");
        jobLogService.insert(jobLog);

        jobInfoService.patchExecuteStatus(job.getId(), DsJobConstants.LAST_EXEC_RUNNING, begin);

        try {
            String msg = "job finished (skeleton): jobId=" + job.getId()
                    + ", datasource=" + job.getDatasourceName()
                    + ", syncType=" + job.getSyncType();
            Date end = new Date();
            jobLog.setExecuteStatus(DsJobConstants.LOG_SUCCESS);
            jobLog.setExecuteEndTime(end);
            jobLog.setTimeConsuming((end.getTime() - begin.getTime()) + "ms");
            jobLog.setHandleDataTotal(0);
            jobLog.setActualUpdateDataTotal(0);
            jobLog.setErrorDataTotal(0);
            jobLog.setJobMessage(msg);
            jobLogService.update(jobLog);

            jobInfoService.patchExecuteStatus(job.getId(), DsJobConstants.LAST_EXEC_SUCCESS, end);
            XxlJobHelper.handleSuccess(msg);
        } catch (Exception e) {
            log.error("datasourceJobHandler failed, jobId={}", job.getId(), e);
            Date end = new Date();
            jobLog.setExecuteStatus(DsJobConstants.LOG_FAIL);
            jobLog.setExecuteEndTime(end);
            jobLog.setTimeConsuming((end.getTime() - begin.getTime()) + "ms");
            jobLog.setJobMessage(e.getMessage());
            jobLogService.update(jobLog);

            jobInfoService.patchExecuteStatus(job.getId(), DsJobConstants.LAST_EXEC_FAIL, end);
            XxlJobHelper.handleFail(e.getMessage());
        }
    }

    private DsJobExecuteParam parseParam(String paramStr) {
        if (paramStr == null || paramStr.trim().isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(paramStr, DsJobExecuteParam.class);
        } catch (Exception e) {
            log.warn("parse executorParam failed: {}", paramStr);
            return null;
        }
    }

    private DsJobInfo resolveJob(DsJobExecuteParam param, long xxlJobId) {
        if (param != null && param.getJobId() != null) {
            return jobInfoService.selectById(param.getJobId());
        }
        return jobInfoService.selectByXxlJobId(xxlJobId);
    }
}
