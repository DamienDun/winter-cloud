package com.winter.cloud.datasource.service;

import com.winter.cloud.datasource.domain.entity.DsJobLog;

import java.util.Date;
import java.util.List;

/**
 * 作业日志
 */
public interface IDsJobLogService {

    List<DsJobLog> selectList(DsJobLog query);

    DsJobLog selectById(Long id);

    int insert(DsJobLog entity);

    int update(DsJobLog entity);

    /** 仅更新执行过程日志，供运行中滚动刷新 */
    int patchJobMessage(Long id, String jobMessage);

    /** 终止执行中的作业（协作式取消 + 调用 XXL kill） */
    void kill(Long id);

    int deleteByIds(Long[] ids);

    long countExecutedSince(Date begin);

    long countFailedExecutionSince(Date begin);

    long countFailedJobSince(Date begin);

    long sumHandleDataSince(Date begin);

    long sumActualUpdateSince(Date begin);

    long sumErrorDataSince(Date begin);
}
