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

    /**
     * 批量开启/停用（同步 XXL-JOB）。
     *
     * @param ids    作业主键
     * @param status 1 开启，0 停用
     * @return 成功处理条数（含已是目标状态而跳过的）
     */
    int changeStatus(Long[] ids, Integer status);

    void execute(Long id);

    List<String> nextTriggerTime(String cron);

    /** Handler 回写最近执行状态（不触发 Feign） */
    void patchExecuteStatus(Long id, Integer lastExecuteStatus, java.util.Date lastExecuteTime);
}
