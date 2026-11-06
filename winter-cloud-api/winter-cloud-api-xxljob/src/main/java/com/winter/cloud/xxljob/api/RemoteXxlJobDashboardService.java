package com.winter.cloud.xxljob.api;

import com.winter.cloud.common.core.constant.ServiceNameConstants;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.xxljob.api.factory.RemoteXxlJobDashboardFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * XXL-JOB 运行报表服务
 */
@FeignClient(contextId = "remoteXxlJobDashboardService", value = ServiceNameConstants.XXLJOB_SERVICE,
        fallbackFactory = RemoteXxlJobDashboardFallbackFactory.class)
public interface RemoteXxlJobDashboardService {

    /**
     * 报表顶部统计
     */
    @GetMapping("/dashboard/info")
    AjaxResult info();

    /**
     * 调度趋势图表
     */
    @GetMapping("/dashboard/chart")
    AjaxResult chart(@RequestParam("startDate") String startDate,
                     @RequestParam("endDate") String endDate);
}
