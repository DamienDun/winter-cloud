package com.winter.cloud.datasource.controller;

import com.winter.cloud.common.core.web.controller.BaseController;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.security.annotation.RequiresPermissions;
import com.winter.cloud.datasource.service.IDsJobStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 作业统计
 */
@RestController
@RequestMapping("/jobStats")
@Tag(name = "作业统计")
public class DsJobStatisticsController extends BaseController {

    @Autowired
    private IDsJobStatisticsService jobStatisticsService;

    @RequiresPermissions("datasource:jobStats:list")
    @GetMapping("/today")
    @Operation(summary = "今日统计")
    public AjaxResult today() {
        return success(jobStatisticsService.statsToday());
    }
}
