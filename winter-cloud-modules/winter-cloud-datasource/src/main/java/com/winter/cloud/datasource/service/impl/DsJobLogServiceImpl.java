package com.winter.cloud.datasource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.datasource.constant.DsJobConstants;
import com.winter.cloud.datasource.domain.entity.DsJobLog;
import com.winter.cloud.datasource.mapper.DsJobLogMapper;
import com.winter.cloud.datasource.service.IDsJobLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 作业日志
 */
@Service
public class DsJobLogServiceImpl implements IDsJobLogService {

    @Autowired
    private DsJobLogMapper jobLogMapper;

    @Override
    public List<DsJobLog> selectList(DsJobLog query) {
        LambdaQueryWrapper<DsJobLog> qw = Wrappers.lambdaQuery();
        if (query != null) {
            qw.eq(query.getJobId() != null, DsJobLog::getJobId, query.getJobId());
            qw.like(StringUtils.isNotEmpty(query.getJobName()), DsJobLog::getJobName, query.getJobName());
            qw.eq(query.getExecuteStatus() != null, DsJobLog::getExecuteStatus, query.getExecuteStatus());
            qw.eq(query.getTriggerType() != null, DsJobLog::getTriggerType, query.getTriggerType());
        }
        qw.orderByDesc(DsJobLog::getExecuteBeginTime);
        return jobLogMapper.selectList(qw);
    }

    @Override
    public DsJobLog selectById(Long id) {
        return jobLogMapper.selectById(id);
    }

    @Override
    public int insert(DsJobLog entity) {
        if (entity.getHandleDataTotal() == null) {
            entity.setHandleDataTotal(0);
        }
        if (entity.getErrorDataTotal() == null) {
            entity.setErrorDataTotal(0);
        }
        if (entity.getActualUpdateDataTotal() == null) {
            entity.setActualUpdateDataTotal(0);
        }
        return jobLogMapper.insert(entity);
    }

    @Override
    public int update(DsJobLog entity) {
        return jobLogMapper.updateById(entity);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        return jobLogMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public long countExecutedSince(Date begin) {
        return jobLogMapper.selectCount(Wrappers.<DsJobLog>lambdaQuery()
                .ge(DsJobLog::getExecuteBeginTime, begin));
    }

    @Override
    public long countFailedExecutionSince(Date begin) {
        return jobLogMapper.selectCount(Wrappers.<DsJobLog>lambdaQuery()
                .ge(DsJobLog::getExecuteBeginTime, begin)
                .eq(DsJobLog::getExecuteStatus, DsJobConstants.LOG_FAIL));
    }

    @Override
    public long countFailedJobSince(Date begin) {
        List<DsJobLog> list = jobLogMapper.selectList(Wrappers.<DsJobLog>lambdaQuery()
                .select(DsJobLog::getJobId)
                .ge(DsJobLog::getExecuteBeginTime, begin)
                .eq(DsJobLog::getExecuteStatus, DsJobConstants.LOG_FAIL));
        return list.stream().map(DsJobLog::getJobId).filter(Objects::nonNull).distinct().count();
    }

    @Override
    public long sumHandleDataSince(Date begin) {
        return sumField(begin, true, false, false);
    }

    @Override
    public long sumActualUpdateSince(Date begin) {
        return sumField(begin, false, true, false);
    }

    @Override
    public long sumErrorDataSince(Date begin) {
        return sumField(begin, false, false, true);
    }

    private long sumField(Date begin, boolean handle, boolean actual, boolean error) {
        List<DsJobLog> list = jobLogMapper.selectList(Wrappers.<DsJobLog>lambdaQuery()
                .ge(DsJobLog::getExecuteBeginTime, begin));
        long sum = 0;
        for (DsJobLog log : list) {
            if (handle) {
                sum += log.getHandleDataTotal() == null ? 0 : log.getHandleDataTotal();
            } else if (actual) {
                sum += log.getActualUpdateDataTotal() == null ? 0 : log.getActualUpdateDataTotal();
            } else if (error) {
                sum += log.getErrorDataTotal() == null ? 0 : log.getErrorDataTotal();
            }
        }
        return sum;
    }
}
