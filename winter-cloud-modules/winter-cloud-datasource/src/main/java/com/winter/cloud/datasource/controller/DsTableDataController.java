package com.winter.cloud.datasource.controller;

import com.winter.cloud.common.core.web.controller.BaseController;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.datasource.info.ColumnInfo;
import com.winter.cloud.common.log.annotation.Log;
import com.winter.cloud.common.log.enums.BusinessType;
import com.winter.cloud.common.security.annotation.RequiresPermissions;
import com.winter.cloud.datasource.domain.dto.DsTableRowRequest;
import com.winter.cloud.datasource.service.IDsTableDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据源表数据维护：选库 → 选表 → 行 CRUD
 */
@RestController
@RequestMapping("/data")
@Tag(name = "数据源表数据")
public class DsTableDataController extends BaseController {

    @Autowired
    private IDsTableDataService tableDataService;

    @RequiresPermissions("datasource:data:list")
    @GetMapping("/tables")
    @Operation(summary = "表名列表")
    public AjaxResult tables(@RequestParam Long datasourceId,
                             @RequestParam(required = false) String tableName) {
        try {
            return success(tableDataService.listTables(datasourceId, tableName));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:data:list")
    @GetMapping("/columns")
    @Operation(summary = "字段列表")
    public AjaxResult columns(@RequestParam Long datasourceId, @RequestParam String tableName) {
        try {
            List<ColumnInfo> list = tableDataService.listColumns(datasourceId, tableName);
            return success(list);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:data:list")
    @GetMapping("/rows")
    @Operation(summary = "分页查询行数据")
    public AjaxResult rows(@RequestParam Long datasourceId,
                           @RequestParam String tableName,
                           @RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            Map<String, Object> page = tableDataService.pageRows(datasourceId, tableName, pageNum, pageSize);
            AjaxResult ajax = AjaxResult.success();
            ajax.put("rows", page.get("rows"));
            ajax.put("total", page.get("total"));
            return ajax;
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:data:add")
    @Log(title = "数据源表数据", businessType = BusinessType.INSERT)
    @PostMapping("/rows")
    @Operation(summary = "新增行")
    public AjaxResult add(@Validated @RequestBody DsTableRowRequest request) {
        try {
            return toAjax(tableDataService.insertRow(request));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:data:edit")
    @Log(title = "数据源表数据", businessType = BusinessType.UPDATE)
    @PutMapping("/rows")
    @Operation(summary = "按主键更新行")
    public AjaxResult edit(@Validated @RequestBody DsTableRowRequest request) {
        try {
            return toAjax(tableDataService.updateRow(request));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:data:remove")
    @Log(title = "数据源表数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/rows")
    @Operation(summary = "按主键删除行")
    public AjaxResult remove(@Validated @RequestBody DsTableRowRequest request) {
        try {
            return toAjax(tableDataService.deleteRow(request));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
