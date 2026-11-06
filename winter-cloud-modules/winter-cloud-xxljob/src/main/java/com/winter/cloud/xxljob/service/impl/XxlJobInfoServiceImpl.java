package com.winter.cloud.xxljob.service.impl;

import com.alibaba.fastjson2.JSON;
import com.winter.cloud.xxljob.domain.XxlJobInfo;
import com.winter.cloud.xxljob.domain.XxlJobLogGlue;
import com.winter.cloud.xxljob.core.thread.JobScheduleHelper;
import com.winter.cloud.xxljob.core.thread.JobTriggerPoolHelper;
import com.winter.cloud.xxljob.core.trigger.TriggerTypeEnum;
import com.winter.cloud.xxljob.core.util.I18nUtil;
import com.winter.cloud.xxljob.mapper.XxlJobInfoMapper;
import com.winter.cloud.xxljob.mapper.XxlJobLogGlueMapper;
import com.winter.cloud.xxljob.service.XxlJobService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.util.DateUtil;
import com.winter.cloud.xxljob.service.IXxlJobInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class XxlJobInfoServiceImpl implements IXxlJobInfoService {

    @Autowired
    private XxlJobService xxlJobService;

    @Autowired
    private XxlJobInfoMapper xxlJobInfoMapper;

    @Autowired
    private XxlJobLogGlueMapper xxlJobLogGlueMapper;

    @Override
    public Map<String, Object> pageList(int pageNum, int pageSize, Integer jobGroup, Integer triggerStatus,
                                        String jobDesc, String executorHandler, String author) {
        int start = Math.max(pageNum - 1, 0) * pageSize;
        Map<String, Object> raw = xxlJobService.pageList(
                start, pageSize,
                jobGroup == null ? 0 : jobGroup,
                triggerStatus == null ? -1 : triggerStatus,
                jobDesc, executorHandler, author);
        Map<String, Object> result = new HashMap<>();
        result.put("total", raw.get("recordsTotal"));
        result.put("rows", raw.get("data"));
        return result;
    }

    @Override
    public Map<String, Object> loadDetail(int id) {
        XxlJobInfo info = xxlJobInfoMapper.loadById(id);
        if (info == null) {
            throw new IllegalStateException(I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("jobInfo", info);
        result.put("jobLogGlues", xxlJobLogGlueMapper.findByJobId(id));
        return result;
    }

    @Override
    public int add(Map<String, Object> job) {
        ReturnT<String> ret = xxlJobService.add(toInfo(job));
        assertOk(ret);
        return Integer.parseInt(ret.getContent());
    }

    @Override
    public void update(Map<String, Object> job) {
        assertOk(xxlJobService.update(toInfo(job)));
    }

    @Override
    public void remove(int id) {
        assertOk(xxlJobService.remove(id));
    }

    @Override
    public void start(int id) {
        assertOk(xxlJobService.start(id));
    }

    @Override
    public void stop(int id) {
        assertOk(xxlJobService.stop(id));
    }

    @Override
    public void trigger(int id, String executorParam, String addressList) {
        if (executorParam == null) {
            executorParam = "";
        }
        JobTriggerPoolHelper.trigger(id, TriggerTypeEnum.MANUAL, -1, null, executorParam, addressList);
    }

    @Override
    public List<String> nextTriggerTime(String scheduleType, String scheduleConf) {
        XxlJobInfo param = new XxlJobInfo();
        param.setScheduleType(scheduleType);
        param.setScheduleConf(scheduleConf);
        List<String> result = new ArrayList<>();
        try {
            Date lastTime = new Date();
            for (int i = 0; i < 5; i++) {
                lastTime = JobScheduleHelper.generateNextValidTime(param, lastTime);
                if (lastTime != null) {
                    result.add(DateUtil.formatDateTime(lastTime));
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException((I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")) + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public void saveGlue(int id, String glueSource, String glueRemark) {
        if (glueRemark == null || glueRemark.trim().length() < 4 || glueRemark.trim().length() > 100) {
            throw new IllegalStateException(I18nUtil.getString("jobinfo_glue_remark_limit"));
        }
        XxlJobInfo exists = xxlJobInfoMapper.loadById(id);
        if (exists == null) {
            throw new IllegalStateException(I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        if (GlueTypeEnum.BEAN == GlueTypeEnum.match(exists.getGlueType())) {
            throw new IllegalStateException(I18nUtil.getString("jobinfo_glue_gluetype_unvalid"));
        }
        exists.setGlueSource(glueSource);
        exists.setGlueRemark(glueRemark.trim());
        exists.setGlueUpdatetime(new Date());
        exists.setUpdateTime(new Date());
        xxlJobInfoMapper.update(exists);

        XxlJobLogGlue logGlue = new XxlJobLogGlue();
        logGlue.setJobId(exists.getId());
        logGlue.setGlueType(exists.getGlueType());
        logGlue.setGlueSource(glueSource);
        logGlue.setGlueRemark(glueRemark.trim());
        logGlue.setAddTime(new Date());
        logGlue.setUpdateTime(new Date());
        xxlJobLogGlueMapper.save(logGlue);
        xxlJobLogGlueMapper.removeOld(exists.getId(), 30);
    }

    private XxlJobInfo toInfo(Map<String, Object> job) {
        return JSON.parseObject(JSON.toJSONString(job), XxlJobInfo.class);
    }

    private void assertOk(ReturnT<String> ret) {
        if (ret == null || ret.getCode() != ReturnT.SUCCESS_CODE) {
            throw new IllegalStateException(ret == null ? "xxl-job error" : ret.getMsg());
        }
    }
}