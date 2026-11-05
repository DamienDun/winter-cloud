package com.winter.cloud.datasource.controller;

import com.winter.cloud.common.core.web.controller.BaseController;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.log.annotation.Log;
import com.winter.cloud.common.log.enums.BusinessType;
import com.winter.cloud.common.security.annotation.RequiresPermissions;
import com.winter.cloud.datasource.domain.dto.DsAlterColumnRequest;
import com.winter.cloud.datasource.domain.dto.DsCreateTableRequest;
import com.winter.cloud.datasource.domain.dto.DsDropTableRequest;
import com.winter.cloud.datasource.domain.dto.DsIndexRequest;
import com.winter.cloud.datasource.service.IDsTableSchemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 表结构 DDL：建表 / 删表 / 改字段 / 索引
 */
@RestController
@RequestMapping("/data/schema")
@Tag(name = "数据源表结构")
public class DsTableSchemaController extends BaseController {

    @Autowired
    private IDsTableSchemaService tableSchemaService;

    @RequiresPermissions("datasource:schema:add")
    @Log(title = "数据源表结构", businessType = BusinessType.INSERT)
    @PostMapping("/table")
    @Operation(summary = "建表")
    public AjaxResult createTable(@Validated @RequestBody DsCreateTableRequest request) {
        try {
            tableSchemaService.createTable(request);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:schema:remove")
    @Log(title = "数据源表结构", businessType = BusinessType.DELETE)
    @DeleteMapping("/table")
    @Operation(summary = "删表（需确认表名）")
    public AjaxResult dropTable(@Validated @RequestBody DsDropTableRequest request) {
        try {
            tableSchemaService.dropTable(request);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:schema:edit")
    @Log(title = "数据源表结构", businessType = BusinessType.UPDATE)
    @PostMapping("/column")
    @Operation(summary = "增改删字段")
    public AjaxResult alterColumn(@Validated @RequestBody DsAlterColumnRequest request) {
        try {
            tableSchemaService.alterColumn(request);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:schema:list")
    @GetMapping("/indexes")
    @Operation(summary = "索引列表")
    public AjaxResult indexes(@RequestParam Long datasourceId, @RequestParam String tableName) {
        try {
            return success(tableSchemaService.listIndexes(datasourceId, tableName));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:schema:edit")
    @Log(title = "数据源表结构", businessType = BusinessType.INSERT)
    @PostMapping("/index")
    @Operation(summary = "创建索引")
    public AjaxResult createIndex(@Validated @RequestBody DsIndexRequest request) {
        try {
            tableSchemaService.createIndex(request);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }


    @RequiresPermissions("datasource:schema:edit")
    @Log(title = "数据源表结构", businessType = BusinessType.UPDATE)
    @PutMapping("/index")
    @Operation(summary = "修改索引（删旧建新）")
    public AjaxResult alterIndex(@Validated @RequestBody DsIndexRequest request) {
        try {
            tableSchemaService.alterIndex(request);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:schema:edit")
    @Log(title = "数据源表结构", businessType = BusinessType.DELETE)
    @DeleteMapping("/index")
    @Operation(summary = "删除索引")
    public AjaxResult dropIndex(@Validated @RequestBody DsIndexRequest request) {
        try {
            tableSchemaService.dropIndex(request);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
