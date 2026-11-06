package com.winter.cloud.xxljob.service.impl;

import com.alibaba.fastjson2.JSON;
import com.winter.cloud.xxljob.core.complete.XxlJobCompleter;
import com.winter.cloud.xxljob.domain.XxlJobInfo;
import com.winter.cloud.xxljob.domain.XxlJobLog;
import com.winter.cloud.xxljob.core.scheduler.XxlJobScheduler;
import com.winter.cloud.xxljob.core.util.I18nUtil;
import com.winter.cloud.xxljob.mapper.XxlJobInfoMapper;
import com.winter.cloud.xxljob.mapper.XxlJobLogMapper;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.KillParam;
import com.xxl.job.core.biz.model.LogParam;
import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.DateUtil;
import com.winter.cloud.xxljob.service.IXxlJobLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class XxlJobLogServiceImpl implements IXxlJobLogService {

    @Autowired
    private XxlJobLogMapper xxlJobLogDao;
    @Autowired
    private XxlJobInfoMapper xxlJobInfoDao;

    @Override
    public Map<String, Object> pageList(int pageNum, int pageSize, Integer jobGroup, Integer jobId,
                                        Integer logStatus, String filterTime) {
        int start = Math.max(pageNum - 1, 0) * pageSize;
        Date triggerTimeStart = null;
        Date triggerTimeEnd = null;
        if (filterTime != null && filterTime.trim().length() > 0) {
            String[] temp = filterTime.split(" - ");
            if (temp.length == 2) {
                triggerTimeStart = DateUtil.parseDateTime(temp[0]);
                triggerTimeEnd = DateUtil.parseDateTime(temp[1]);
            }
        }
        int group = jobGroup == null ? 0 : jobGroup;
        int id = jobId == null ? 0 : jobId;
        int status = logStatus == null ? -1 : logStatus;
        List<XxlJobLog> list = xxlJobLogDao.pageList(start, pageSize, group, id, triggerTimeStart, triggerTimeEnd, status);
        int count = xxlJobLogDao.pageListCount(start, pageSize, group, id, triggerTimeStart, triggerTimeEnd, status);
        Map<String, Object> result = new HashMap<>();
        result.put("total", count);
        result.put("rows", list);
        return result;
    }

    @Override
    public List<Map<String, Object>> getJobsByGroup(int jobGroup) {
        List<XxlJobInfo> list = xxlJobInfoDao.getJobsByGroup(jobGroup);
        if (list == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (XxlJobInfo info : list) {
            rows.add(JSON.parseObject(JSON.toJSONString(info)));
        }
        return rows;
    }

    @Override
    public Map<String, Object> logDetailCat(String executorAddress, long triggerTime, long logId, int fromLineNum) {
        if (!StringUtils.hasText(executorAddress)) {
            return failLog("executorAddress is empty; cannot load executor log", fromLineNum);
        }
        try {
            ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(executorAddress);
            ReturnT<LogResult> logResult = executorBiz.log(new LogParam(triggerTime, logId, fromLineNum));
            if (logResult == null) {
                return failLog("empty log response", fromLineNum);
            }
            if (logResult.getCode() != ReturnT.SUCCESS_CODE) {
                return failLog(logResult.getMsg() == null ? "load log failed" : logResult.getMsg(), fromLineNum);
            }
            LogResult content = logResult.getContent();
            if (content != null && content.getFromLineNum() > content.getToLineNum()) {
                XxlJobLog jobLog = xxlJobLogDao.load(logId);
                if (jobLog != null && jobLog.getHandleCode() > 0) {
                    content.setEnd(true);
                }
            }
            if (content == null) {
                return failLog("empty log content", fromLineNum);
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("fromLineNum", content.getFromLineNum());
            map.put("toLineNum", content.getToLineNum());
            map.put("logContent", content.getLogContent());
            map.put("end", content.isEnd());
            return map;
        } catch (Exception e) {
            return failLog(e.getMessage(), fromLineNum);
        }
    }

    private Map<String, Object> failLog(String msg, int fromLineNum) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("logContent", msg);
        m.put("fromLineNum", fromLineNum);
        m.put("toLineNum", fromLineNum);
        m.put("end", true);
        return m;
    }

    @Override
    public void clearLog(int jobGroup, int jobId, int type) {
        Date clearBeforeTime = null;
        int clearBeforeNum = 0;
        if (type == 1) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -1);
        } else if (type == 2) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -3);
        } else if (type == 3) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -6);
        } else if (type == 4) {
            clearBeforeTime = DateUtil.addYears(new Date(), -1);
        } else if (type == 5) {
            clearBeforeNum = 1000;
        } else if (type == 6) {
            clearBeforeNum = 10000;
        } else if (type == 7) {
            clearBeforeNum = 30000;
        } else if (type == 8) {
            clearBeforeNum = 100000;
        } else if (type == 9) {
            clearBeforeNum = 0;
        } else {
            throw new IllegalStateException(I18nUtil.getString("joblog_clean_type_unvalid"));
        }
        List<Long> logIds;
        do {
            logIds = xxlJobLogDao.findClearLogIds(jobGroup, jobId, clearBeforeTime, clearBeforeNum, 1000);
            if (logIds != null && !logIds.isEmpty()) {
                xxlJobLogDao.clearLog(logIds);
            }
        } while (logIds != null && !logIds.isEmpty());
    }

    @Override
    public void kill(int id) {
        XxlJobLog log = xxlJobLogDao.load(id);
        if (log == null) {
            throw new IllegalStateException(I18nUtil.getString("joblog_logid_unvalid"));
        }
        XxlJobInfo jobInfo = xxlJobInfoDao.loadById(log.getJobId());
        if (jobInfo == null) {
            throw new IllegalStateException(I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        if (ReturnT.SUCCESS_CODE != log.getTriggerCode()) {
            throw new IllegalStateException(I18nUtil.getString("joblog_kill_log_limit"));
        }
        ReturnT<String> runResult;
        try {
            ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(log.getExecutorAddress());
            runResult = executorBiz.kill(new KillParam(jobInfo.getId()));
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        if (ReturnT.SUCCESS_CODE == runResult.getCode()) {
            log.setHandleCode(ReturnT.FAIL_CODE);
            log.setHandleMsg(I18nUtil.getString("joblog_kill_log_byman") + ":" + (runResult.getMsg() != null ? runResult.getMsg() : ""));
            log.setHandleTime(new Date());
            XxlJobCompleter.updateHandleInfoAndFinish(log);
        } else {
            throw new IllegalStateException(runResult.getMsg());
        }
    }
}