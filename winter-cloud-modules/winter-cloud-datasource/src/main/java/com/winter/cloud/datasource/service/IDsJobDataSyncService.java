package com.winter.cloud.datasource.service;

import com.winter.cloud.datasource.domain.dto.DsJobSyncResult;
import com.winter.cloud.datasource.domain.entity.DsJobInfo;

/**
 * 作业数据同步：源(API/JDBC) → 目标 JDBC，缺表建表、缺列补列
 */
public interface IDsJobDataSyncService {

    DsJobSyncResult sync(DsJobInfo job);
}
