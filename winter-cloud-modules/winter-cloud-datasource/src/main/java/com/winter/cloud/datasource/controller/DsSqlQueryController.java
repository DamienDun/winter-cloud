package com.winter.cloud.datasource.controller;

import com.winter.cloud.common.core.web.controller.BaseController;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.log.annotation.Log;
import com.winter.cloud.common.log.enums.BusinessType;
import com.winter.cloud.common.security.annotation.RequiresPermissions;
import com.winter.cloud.datasource.domain.dto.DsSqlQueryRequest;
import com.winter.cloud.datasource.service.IDsSqlQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Custom readonly SQL query API
 */
@RestController
@RequestMapping("/sql")
@Tag(name = "SQLQuery")
public class DsSqlQueryController extends BaseController {

    @Autowired
    private IDsSqlQueryService sqlQueryService;

    @RequiresPermissions("datasource:sql:query")
    @Log(title = "SQLQuery", businessType = BusinessType.OTHER)
    @PostMapping("/query")
    @Operation(summary = "run readonly SQL")
    public AjaxResult query(@Validated @RequestBody DsSqlQueryRequest request) {
        try {
            return success(sqlQueryService.query(request));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}