package com.winter.cloud.datasource.service;

import com.winter.cloud.datasource.domain.dto.DsJobSyncResult;
import com.winter.cloud.datasource.domain.entity.DsJobInfo;

import java.util.function.Consumer;

/**
 * 作业数据同步：源(API/JDBC) → 目标 JDBC，缺表建表、缺列补列
 */
public interface IDsJobDataSyncService {

    DsJobSyncResult sync(DsJobInfo job);

    /**
     * @param jobLogId     作业日志 ID，用于协作式终止检查；可空
     * @param progressSink 分步日志进度回调（已节流），供前端轮询；可空
     */
    DsJobSyncResult sync(DsJobInfo job, Long jobLogId, Consumer<String> progressSink);
}
