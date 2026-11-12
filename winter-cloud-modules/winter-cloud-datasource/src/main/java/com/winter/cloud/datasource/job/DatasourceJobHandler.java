package com.winter.cloud.datasource.job;

import com.alibaba.fastjson2.JSON;
import com.winter.cloud.datasource.constant.DsJobConstants;
import com.winter.cloud.datasource.domain.dto.DsJobExecuteParam;
import com.winter.cloud.datasource.domain.dto.DsJobSyncException;
import com.winter.cloud.datasource.domain.dto.DsJobSyncResult;
import com.winter.cloud.datasource.domain.dto.DsJobSyncTrace;
import com.winter.cloud.datasource.domain.entity.DsJobInfo;
import com.winter.cloud.datasource.domain.entity.DsJobLog;
import com.winter.cloud.datasource.service.IDsJobDataSyncService;
import com.winter.cloud.datasource.service.IDsJobInfoService;
import com.winter.cloud.datasource.service.IDsJobLogService;
import com.winter.cloud.datasource.support.DsJobCancelRegistry;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 数据源作业执行器：从 API/JDBC 源取数，按字段映射写入目标 JDBC（缺表建表、缺列补列）
 */
@Component
public class DatasourceJobHandler {

    private static final Logger log = LoggerFactory.getLogger(DatasourceJobHandler.class);

    @Autowired
    private IDsJobInfoService jobInfoService;

    @Autowired
    private IDsJobLogService jobLogService;

    @Autowired
    private IDsJobDataSyncService jobDataSyncService;

    @Autowired
    private DsJobCancelRegistry jobCancelRegistry;

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
        int triggerType = param != null && param.getTriggerType() != null
                ? param.getTriggerType() : DsJobConstants.TRIGGER_CRON;
        String triggerUser = param != null && param.getTriggerUsername() != null
                ? param.getTriggerUsername() : "xxl-job";

        DsJobSyncTrace bootTrace = new DsJobSyncTrace();
        bootTrace.step("执行器收到任务：xxlJobId=%d param=%s", xxlJobId, paramStr == null ? "" : paramStr);
        bootTrace.step("解析作业：jobId=%s name=%s sourceType=%s sourceDs=%s targetDs=%s targetTable=%s",
                job.getId(), job.getJobName(), job.getDatasourceType(),
                job.getDatasourceName(), job.getTargetDatasourceName(), job.getTargetTable());
        bootTrace.step("触发信息：triggerType=%s user=%s mappingCount=%d",
                triggerType == DsJobConstants.TRIGGER_CRON ? "定时" : "手动",
                triggerUser,
                job.getFieldMappings() == null ? 0 : job.getFieldMappings().size());
        bootTrace.step("开始执行同步...");

        DsJobLog jobLog = new DsJobLog();
        jobLog.setJobId(job.getId());
        jobLog.setJobName(job.getJobName());
        jobLog.setTriggerType(triggerType);
        jobLog.setTriggerUserId(param != null ? param.getTriggerUserId() : null);
        jobLog.setTriggerUsername(triggerUser);
        jobLog.setExecuteStatus(DsJobConstants.LOG_RUNNING);
        jobLog.setExecuteBeginTime(begin);
        jobLog.setDatasourceType(job.getDatasourceType());
        jobLog.setJobMessage(bootTrace.toString());
        Long xxlLogId = currentXxlLogId();
        jobLog.setXxlLogId(xxlLogId);
        if (xxlLogId != null) {
            bootTrace.step("关联 XXL 调度日志：xxlLogId=%d", xxlLogId);
            jobLog.setJobMessage(bootTrace.toString());
        }
        jobLogService.insert(jobLog);

        jobInfoService.patchExecuteStatus(job.getId(), DsJobConstants.LAST_EXEC_RUNNING, begin);

        try {
            DsJobSyncResult syncResult = jobDataSyncService.sync(job, jobLog.getId(), syncDetail ->
                    jobLogService.patchJobMessage(jobLog.getId(), mergeBootAndDetail(bootTrace, syncDetail)));
            Date end = new Date();
            String summary = syncResult.getMessage() == null ? "job finished" : syncResult.getMessage();
            String detail = mergeBootAndDetail(bootTrace, syncResult.getDetailLog());
            jobLog.setExecuteStatus(DsJobConstants.LOG_SUCCESS);
            jobLog.setExecuteEndTime(end);
            jobLog.setTimeConsuming((end.getTime() - begin.getTime()) + "ms");
            jobLog.setHandleDataTotal(syncResult.getSourceCount());
            jobLog.setActualUpdateDataTotal(syncResult.getInsertedCount());
            jobLog.setErrorDataTotal(syncResult.getErrorCount());
            jobLog.setJobMessage(detail);
            jobLogService.update(jobLog);

            jobInfoService.patchExecuteStatus(job.getId(), DsJobConstants.LAST_EXEC_SUCCESS, end);
            XxlJobHelper.handleSuccess(summary);
        } catch (Exception e) {
            log.error("datasourceJobHandler failed, jobId={}", job.getId(), e);
            Date end = new Date();
            String syncDetail;
            if (e instanceof DsJobSyncException) {
                syncDetail = ((DsJobSyncException) e).getDetailLog();
            } else {
                DsJobSyncTrace failTrace = new DsJobSyncTrace();
                failTrace.step("执行器捕获异常，同步中断");
                failTrace.fail(e);
                syncDetail = failTrace.toString();
            }
            String detail = mergeBootAndDetail(bootTrace, syncDetail);
            boolean cancelled = jobCancelRegistry.isCancelRequested(jobLog.getId())
                    || (e.getMessage() != null && (e.getMessage().contains("手动终止")
                    || e.getMessage().contains("cancelled by user")));
            jobLog.setExecuteStatus(DsJobConstants.LOG_FAIL);
            jobLog.setExecuteEndTime(end);
            jobLog.setTimeConsuming((end.getTime() - begin.getTime()) + "ms");
            jobLog.setJobMessage(cancelled ? detail + "\n[任务已手动终止]\n" : detail);
            jobLogService.update(jobLog);

            jobInfoService.patchExecuteStatus(job.getId(), DsJobConstants.LAST_EXEC_FAIL, end);
            XxlJobHelper.handleFail(cancelled ? "任务已被手动终止" : e.getMessage());
        } finally {
            jobCancelRegistry.clear(jobLog.getId());
        }
    }

    /**
     * xxl-job 2.3 无 getJobLogId，日志文件名形如 .../yyyy-MM-dd/{logId}.log
     */
    private Long currentXxlLogId() {
        try {
            String fileName = XxlJobHelper.getJobLogFileName();
            if (fileName == null || fileName.isEmpty()) {
                return null;
            }
            String normalized = fileName.replace('\\', '/');
            int slash = normalized.lastIndexOf('/');
            String file = slash >= 0 ? normalized.substring(slash + 1) : normalized;
            if (!file.endsWith(".log")) {
                return null;
            }
            String idPart = file.substring(0, file.length() - 4);
            long logId = Long.parseLong(idPart);
            return logId > 0 ? logId : null;
        } catch (Throwable e) {
            return null;
        }
    }

    /** 把执行器引导日志与同步分步日志拼成完整 jobMessage */
    private String mergeBootAndDetail(DsJobSyncTrace bootTrace, String syncDetail) {
        StringBuilder sb = new StringBuilder();
        if (bootTrace != null) {
            sb.append(bootTrace);
        }
        if (syncDetail != null && !syncDetail.isEmpty()) {
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
                sb.append('\n');
            }
            sb.append("--- 同步过程 ---\n");
            sb.append(syncDetail);
        }
        String all = sb.toString();
        if (all.length() > 60000) {
            return all.substring(0, 60000) + "\n... (日志过长已截断)\n";
        }
        return all;
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
