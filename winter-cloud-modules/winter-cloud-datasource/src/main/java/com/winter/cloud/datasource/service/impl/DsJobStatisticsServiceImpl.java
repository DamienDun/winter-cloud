package com.winter.cloud.datasource.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.winter.cloud.datasource.constant.DsJobConstants;
import com.winter.cloud.datasource.domain.entity.DsJobInfo;
import com.winter.cloud.datasource.domain.entity.DsJobStatistics;
import com.winter.cloud.datasource.mapper.DsJobInfoMapper;
import com.winter.cloud.datasource.service.IDsJobLogService;
import com.winter.cloud.datasource.service.IDsJobStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

/**
 * 作业统计：按今日日志实时汇总（pending 取当前启用中的定时作业数）
 */
@Service
public class DsJobStatisticsServiceImpl implements IDsJobStatisticsService {

    @Autowired
    private DsJobInfoMapper jobInfoMapper;

    @Autowired
    private IDsJobLogService jobLogService;

    @Override
    public DsJobStatistics statsToday() {
        Date begin = dayBegin(new Date());
        DsJobStatistics stats = new DsJobStatistics();
        stats.setDateTime(begin);
        long pending = jobInfoMapper.selectCount(Wrappers.<DsJobInfo>lambdaQuery()
                .eq(DsJobInfo::getJobStatus, DsJobConstants.JOB_STATUS_ENABLED)
                .eq(DsJobInfo::getTriggerType, DsJobConstants.TRIGGER_CRON));
        stats.setPendingJob((int) pending);
        stats.setExecuted((int) jobLogService.countExecutedSince(begin));
        stats.setFailedExecution((int) jobLogService.countFailedExecutionSince(begin));
        stats.setFailedJob((int) jobLogService.countFailedJobSince(begin));
        stats.setHandleDataTotal((int) jobLogService.sumHandleDataSince(begin));
        stats.setActualUpdateDataTotal((int) jobLogService.sumActualUpdateSince(begin));
        stats.setErrorDataTotal((int) jobLogService.sumErrorDataSince(begin));
        return stats;
    }

    private Date dayBegin(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
