package com.winter.cloud.datasource.service;

import com.winter.cloud.datasource.domain.entity.DsJobInfo;

import java.util.List;

/**
 * 作业信息
 */
public interface IDsJobInfoService {

    List<DsJobInfo> selectList(DsJobInfo query);

    DsJobInfo selectById(Long id);

    DsJobInfo selectByXxlJobId(Long xxlJobId);

    int insert(DsJobInfo entity);

    int update(DsJobInfo entity);

    int deleteByIds(Long[] ids);

    void start(Long id);

    void stop(Long id);

    void execute(Long id);

    List<String> nextTriggerTime(String cron);

    /** Handler 回写最近执行状态（不触发 Feign） */
    void patchExecuteStatus(Long id, Integer lastExecuteStatus, java.util.Date lastExecuteTime);
}
