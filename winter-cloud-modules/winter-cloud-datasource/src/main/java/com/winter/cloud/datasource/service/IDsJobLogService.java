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

    int deleteByIds(Long[] ids);

    long countExecutedSince(Date begin);

    long countFailedExecutionSince(Date begin);

    long countFailedJobSince(Date begin);

    long sumHandleDataSince(Date begin);

    long sumActualUpdateSince(Date begin);

    long sumErrorDataSince(Date begin);
}
