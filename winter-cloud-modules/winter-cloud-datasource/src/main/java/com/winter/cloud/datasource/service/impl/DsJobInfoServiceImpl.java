package com.winter.cloud.datasource.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.security.utils.SecurityUtils;
import com.winter.cloud.datasource.constant.DsJobConstants;
import com.winter.cloud.datasource.domain.dto.DsJobExecuteParam;
import com.winter.cloud.datasource.domain.entity.DsJdbcDatasource;
import com.winter.cloud.datasource.domain.entity.DsJobInfo;
import com.winter.cloud.datasource.mapper.DsJobInfoMapper;
import com.winter.cloud.datasource.service.IDsJdbcDatasourceService;
import com.winter.cloud.datasource.service.IDsJobInfoService;
import com.winter.cloud.datasource.support.XxlJobBindHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Job info: local persist + Feign sync to XXL-JOB, save xxlJobId
 */
@Service
public class DsJobInfoServiceImpl implements IDsJobInfoService {

    @Autowired
    private DsJobInfoMapper jobInfoMapper;

    @Autowired
    private IDsJdbcDatasourceService datasourceService;

    @Autowired
    private XxlJobBindHelper xxlJobBindHelper;

    @Override
    public List<DsJobInfo> selectList(DsJobInfo query) {
        LambdaQueryWrapper<DsJobInfo> qw = Wrappers.lambdaQuery();
        if (query != null) {
            qw.like(StringUtils.isNotEmpty(query.getJobName()), DsJobInfo::getJobName, query.getJobName());
            qw.eq(query.getJobStatus() != null, DsJobInfo::getJobStatus, query.getJobStatus());
            qw.eq(query.getDatasourceId() != null, DsJobInfo::getDatasourceId, query.getDatasourceId());
            qw.eq(query.getTriggerType() != null, DsJobInfo::getTriggerType, query.getTriggerType());
            qw.eq(query.getSyncType() != null, DsJobInfo::getSyncType, query.getSyncType());
            qw.eq(query.getLastExecuteStatus() != null, DsJobInfo::getLastExecuteStatus, query.getLastExecuteStatus());
        }
        qw.orderByDesc(DsJobInfo::getGmtModified);
        return jobInfoMapper.selectList(qw);
    }

    @Override
    public DsJobInfo selectById(Long id) {
        return jobInfoMapper.selectById(id);
    }

    @Override
    public DsJobInfo selectByXxlJobId(Long xxlJobId) {
        if (xxlJobId == null) {
            return null;
        }
        return jobInfoMapper.selectOne(Wrappers.<DsJobInfo>lambdaQuery()
                .eq(DsJobInfo::getXxlJobId, xxlJobId)
                .last("limit 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(DsJobInfo entity) {
        fillDatasourceSnapshot(entity);
        normalizeDefaults(entity);
        int rows = jobInfoMapper.insert(entity);
        Long xxlJobId = xxlJobBindHelper.createXxlJob(entity);
        entity.setXxlJobId(xxlJobId);
        jobInfoMapper.updateById(entity);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(DsJobInfo entity) {
        DsJobInfo db = requireJob(entity.getId());
        fillDatasourceSnapshot(entity);
        entity.setXxlJobId(db.getXxlJobId());
        int rows = jobInfoMapper.updateById(entity);
        if (db.getXxlJobId() != null) {
            DsJobInfo merged = jobInfoMapper.selectById(entity.getId());
            xxlJobBindHelper.updateXxlJob(merged);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(Long[] ids) {
        int n = 0;
        for (Long id : ids) {
            DsJobInfo job = jobInfoMapper.selectById(id);
            if (job == null) {
                continue;
            }
            if (Integer.valueOf(DsJobConstants.JOB_STATUS_ENABLED).equals(job.getJobStatus())) {
                throw new IllegalStateException("stop job before delete: " + job.getJobName());
            }
            xxlJobBindHelper.removeXxlJob(job.getXxlJobId());
            n += jobInfoMapper.deleteById(id);
        }
        return n;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void start(Long id) {
        DsJobInfo job = requireJob(id);
        if (job.getXxlJobId() == null) {
            throw new IllegalStateException("job not bound to xxl-job");
        }
        if (Integer.valueOf(DsJobConstants.TRIGGER_CRON).equals(job.getTriggerType())
                && StringUtils.isEmpty(job.getCron())) {
            throw new IllegalStateException("cron required for schedule job");
        }
        xxlJobBindHelper.updateXxlJob(job);
        xxlJobBindHelper.startXxlJob(job.getXxlJobId());
        job.setJobStatus(DsJobConstants.JOB_STATUS_ENABLED);
        jobInfoMapper.updateById(job);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stop(Long id) {
        DsJobInfo job = requireJob(id);
        if (job.getXxlJobId() != null) {
            xxlJobBindHelper.stopXxlJob(job.getXxlJobId());
        }
        job.setJobStatus(DsJobConstants.JOB_STATUS_DISABLED);
        jobInfoMapper.updateById(job);
    }

    @Override
    public void execute(Long id) {
        DsJobInfo job = requireJob(id);
        if (job.getXxlJobId() == null) {
            throw new IllegalStateException("job not bound to xxl-job");
        }
        DsJobExecuteParam param = new DsJobExecuteParam(
                job.getId(),
                DsJobConstants.TRIGGER_MANUAL,
                SecurityUtils.getUserId(),
                SecurityUtils.getUsername());
        xxlJobBindHelper.triggerXxlJob(job.getXxlJobId(), JSON.toJSONString(param));
    }

    @Override
    public List<String> nextTriggerTime(String cron) {
        return xxlJobBindHelper.nextTriggerTime(cron);
    }

    @Override
    public void patchExecuteStatus(Long id, Integer lastExecuteStatus, Date lastExecuteTime) {
        DsJobInfo patch = new DsJobInfo();
        patch.setId(id);
        patch.setLastExecuteStatus(lastExecuteStatus);
        patch.setLastExecuteTime(lastExecuteTime);
        jobInfoMapper.updateById(patch);
    }

    private DsJobInfo requireJob(Long id) {
        DsJobInfo job = jobInfoMapper.selectById(id);
        if (job == null) {
            throw new IllegalStateException("job not found");
        }
        return job;
    }

    private void fillDatasourceSnapshot(DsJobInfo entity) {
        if (entity.getDatasourceId() == null) {
            throw new IllegalArgumentException("datasourceId required");
        }
        DsJdbcDatasource ds = datasourceService.selectById(entity.getDatasourceId());
        if (ds == null) {
            throw new IllegalArgumentException("datasource not found");
        }
        entity.setDatasourceName(ds.getDatasourceName());
        entity.setDatasourceType(ds.getDatasourceType());
    }

    private void normalizeDefaults(DsJobInfo entity) {
        if (entity.getJobStatus() == null) {
            entity.setJobStatus(DsJobConstants.JOB_STATUS_DISABLED);
        }
        if (entity.getLastExecuteStatus() == null) {
            entity.setLastExecuteStatus(DsJobConstants.LAST_EXEC_NONE);
        }
        if (entity.getSyncType() == null) {
            entity.setSyncType(DsJobConstants.SYNC_FULL);
        }
        if (entity.getTriggerType() == null) {
            entity.setTriggerType(DsJobConstants.TRIGGER_MANUAL);
        }
        if (entity.getCron() == null) {
            entity.setCron("");
        }
        if (entity.getLastExecuteTime() == null) {
            entity.setLastExecuteTime(new Date(0));
        }
    }
}
