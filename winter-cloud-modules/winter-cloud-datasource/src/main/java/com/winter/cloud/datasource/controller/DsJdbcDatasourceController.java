package com.winter.cloud.datasource.controller;

import com.winter.cloud.common.core.constant.Constants;
import com.winter.cloud.common.core.web.controller.BaseController;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.core.web.page.TableDataInfo;
import com.winter.cloud.common.log.annotation.Log;
import com.winter.cloud.common.log.enums.BusinessType;
import com.winter.cloud.common.security.annotation.RequiresPermissions;
import com.winter.cloud.datasource.domain.entity.DsJdbcDatasource;
import com.winter.cloud.datasource.service.IDsJdbcDatasourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 数据源管理（参考 datax-web jobJdbcDatasource）
 */
@RestController
@RequestMapping("/info")
@Tag(name = "数据源管理")
public class DsJdbcDatasourceController extends BaseController {

    @Autowired
    private IDsJdbcDatasourceService datasourceService;

    @RequiresPermissions("datasource:info:list")
    @GetMapping("/list")
    @Operation(summary = "分页查询")
    public TableDataInfo list(DsJdbcDatasource query) {
        startPage();
        List<DsJdbcDatasource> list = datasourceService.selectList(query);
        // 列表不返回密码
        list.forEach(i -> i.setJdbcPassword(null));
        return getDataTable(list);
    }

    @RequiresPermissions("datasource:info:query")
    @GetMapping("/{id}")
    @Operation(summary = "详情")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(datasourceService.selectById(id));
    }

    @RequiresPermissions("datasource:info:list")
    @GetMapping("/all")
    @Operation(summary = "启用中的全部数据源（下拉）")
    public AjaxResult all() {
        List<DsJdbcDatasource> list = datasourceService.selectAllEnabled();
        list.forEach(i -> i.setJdbcPassword(null));
        return success(list);
    }

    @RequiresPermissions("datasource:info:add")
    @Log(title = "数据源管理", businessType = BusinessType.INSERT)
    @PostMapping
    @Operation(summary = "新增")
    public AjaxResult add(@Validated @RequestBody DsJdbcDatasource entity) {
        return toAjax(datasourceService.insert(entity));
    }

    @RequiresPermissions("datasource:info:edit")
    @Log(title = "数据源管理", businessType = BusinessType.UPDATE)
    @PutMapping
    @Operation(summary = "修改")
    public AjaxResult edit(@Validated @RequestBody DsJdbcDatasource entity) {
        return toAjax(datasourceService.update(entity));
    }

    @RequiresPermissions("datasource:info:remove")
    @Log(title = "数据源管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    @Operation(summary = "删除")
    public AjaxResult remove(@PathVariable String ids) {
        Long[] idArr = Arrays.stream(ids.split(Constants.ENGLISH_COMMA))
                .map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).toArray(Long[]::new);
        return toAjax(datasourceService.deleteByIds(idArr));
    }

    @RequiresPermissions("datasource:info:test")
    @PostMapping("/test")
    @Operation(summary = "测试连接")
    public AjaxResult test(@RequestBody DsJdbcDatasource entity) {
        try {
            boolean ok = datasourceService.testConnection(entity);
            return ok ? success(true) : error("连接失败");
        } catch (Exception e) {
            return error("连接失败：" + e.getMessage());
        }
    }
}
