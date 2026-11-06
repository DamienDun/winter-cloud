package com.winter.cloud.xxljob.service;

import java.util.Map;

/**
 * Dashboard APIs aligned with xxl-job-admin index page
 */
public interface IXxlJobDashboardService {

    /** Top cards: job count / trigger count / online executor count */
    Map<String, Object> dashboardInfo();

    /** Date distribution + success ratio chart data */
    Map<String, Object> chartInfo(String startDate, String endDate);
}