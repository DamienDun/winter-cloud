package com.winter.cloud.datasource.controller;

import com.winter.cloud.common.core.constant.Constants;
import com.winter.cloud.common.core.web.controller.BaseController;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.core.web.page.TableDataInfo;
import com.winter.cloud.common.log.annotation.Log;
import com.winter.cloud.common.log.enums.BusinessType;
import com.winter.cloud.common.security.annotation.RequiresPermissions;
import com.winter.cloud.datasource.domain.dto.DsApiTestResult;
import com.winter.cloud.datasource.domain.entity.DsApiDatasource;
import com.winter.cloud.datasource.service.IDsApiDatasourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Tag(name = "API datasource")
public class DsApiDatasourceController extends BaseController {

    @Autowired
    private IDsApiDatasourceService apiDatasourceService;

    @RequiresPermissions("datasource:api:list")
    @GetMapping("/list")
    @Operation(summary = "page list")
    public TableDataInfo list(DsApiDatasource query) {
        startPage();
        List<DsApiDatasource> list = apiDatasourceService.selectList(query);
        return getDataTable(list);
    }

    /**
     * 依赖候选列表（勿用 /relyCandidates，易与 /{id} 冲突被当成主键解析）
     * id 一律返回字符串，避免雪花 Long 在前端精度丢失。
     */
    @RequiresPermissions(value = {"datasource:api:list", "datasource:api:query", "datasource:api:add", "datasource:api:edit"}, logical = com.winter.cloud.common.security.annotation.Logical.OR)
    @GetMapping("/rely/list")
    @Operation(summary = "rely datasource candidates with fuzzy name")
    public AjaxResult relyList(@RequestParam(value = "excludeId", required = false) String excludeId,
                               @RequestParam(value = "datasourceName", required = false) String datasourceName) {
        Long exclude = null;
        if (excludeId != null && !excludeId.trim().isEmpty()) {
            exclude = Long.valueOf(excludeId.trim());
        }
        List<DsApiDatasource> list = apiDatasourceService.selectRelyCandidates(exclude, datasourceName);
        List<Map<String, Object>> vos = list.stream().map(e -> {
            Map<String, Object> m = new HashMap<>(4);
            m.put("id", e.getId() == null ? null : String.valueOf(e.getId()));
            m.put("datasourceName", e.getDatasourceName());
            m.put("reqType", e.getReqType());
            m.put("apiUrl", e.getApiUrl());
            return m;
        }).collect(Collectors.toList());
        return success(vos);
    }

    @RequiresPermissions("datasource:api:query")
    @GetMapping("/fields/{id}")
    @Operation(summary = "list output fields of an API datasource")
    public AjaxResult fields(@PathVariable("id") Long id) {
        try {
            return success(apiDatasourceService.listOutputFields(id));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:api:query")
    @GetMapping("/{id}")
    @Operation(summary = "detail")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(apiDatasourceService.selectById(id));
    }

    @RequiresPermissions("datasource:api:add")
    @Log(title = "API datasource", businessType = BusinessType.INSERT)
    @PostMapping
    @Operation(summary = "add")
    public AjaxResult add(@RequestBody DsApiDatasource entity) {
        try {
            return toAjax(apiDatasourceService.insert(entity));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:api:edit")
    @Log(title = "API datasource", businessType = BusinessType.UPDATE)
    @PutMapping
    @Operation(summary = "edit")
    public AjaxResult edit(@RequestBody DsApiDatasource entity) {
        try {
            return toAjax(apiDatasourceService.update(entity));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:api:remove")
    @Log(title = "API datasource", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    @Operation(summary = "delete")
    public AjaxResult remove(@PathVariable String ids) {
        try {
            Long[] idArr = Arrays.stream(ids.split(Constants.ENGLISH_COMMA))
                    .map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).toArray(Long[]::new);
            return toAjax(apiDatasourceService.deleteByIds(idArr));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:api:test")
    @PostMapping("/test")
    @Operation(summary = "test connection")
    public AjaxResult test(@RequestBody DsApiDatasource entity) {
        DsApiTestResult result = apiDatasourceService.testConnection(entity);
        if (result.getTestStatus() != null && result.getTestStatus() == 0) {
            return success(result);
        }
        AjaxResult err = error(result.getTestResult() == null ? "connection failed" : result.getTestResult());
        err.put("data", result);
        return err;
    }
}
