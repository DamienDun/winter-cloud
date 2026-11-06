package com.winter.cloud.xxljob.controller;

import com.winter.cloud.common.core.web.controller.BaseController;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.security.annotation.RequiresPermissions;
import com.winter.cloud.xxljob.service.IXxlJobDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "XXL-JOB运行报表")
public class XxlJobDashboardController extends BaseController {

    @Autowired
    private IXxlJobDashboardService dashboardService;

    @RequiresPermissions("xxljob:dashboard:list")
    @GetMapping("/info")
    @Operation(summary = "报表顶部统计")
    public AjaxResult info() {
        try {
            return success(dashboardService.dashboardInfo());
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("xxljob:dashboard:list")
    @GetMapping("/chart")
    @Operation(summary = "调度趋势图表")
    public AjaxResult chart(@RequestParam String startDate, @RequestParam String endDate) {
        try {
            return success(dashboardService.chartInfo(startDate, endDate));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
