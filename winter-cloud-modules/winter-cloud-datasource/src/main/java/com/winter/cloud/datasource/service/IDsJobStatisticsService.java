package com.winter.cloud.datasource.service;

import com.winter.cloud.datasource.domain.entity.DsJobStatistics;

/**
 * 作业统计
 */
public interface IDsJobStatisticsService {

    /** 实时汇总今日统计（含启用中的定时作业数作为 pending） */
    DsJobStatistics statsToday();
}
