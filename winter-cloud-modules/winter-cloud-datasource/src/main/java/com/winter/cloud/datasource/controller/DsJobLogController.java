package com.winter.cloud.datasource.controller;

import com.winter.cloud.common.core.constant.Constants;
import com.winter.cloud.common.core.web.controller.BaseController;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.core.web.page.TableDataInfo;
import com.winter.cloud.common.log.annotation.Log;
import com.winter.cloud.common.log.enums.BusinessType;
import com.winter.cloud.common.security.annotation.RequiresPermissions;
import com.winter.cloud.datasource.domain.entity.DsJobLog;
import com.winter.cloud.datasource.service.IDsJobLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 作业日志
 */
@RestController
@RequestMapping("/jobLog")
@Tag(name = "作业日志")
public class DsJobLogController extends BaseController {

    @Autowired
    private IDsJobLogService jobLogService;

    @RequiresPermissions("datasource:jobLog:list")
    @GetMapping("/list")
    @Operation(summary = "分页查询")
    public TableDataInfo list(DsJobLog query) {
        startPage();
        List<DsJobLog> list = jobLogService.selectList(query);
        return getDataTable(list);
    }

    @RequiresPermissions("datasource:jobLog:query")
    @GetMapping("/{id}")
    @Operation(summary = "详情")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(jobLogService.selectById(id));
    }

    @RequiresPermissions("datasource:jobLog:remove")
    @Log(title = "作业日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    @Operation(summary = "删除")
    public AjaxResult remove(@PathVariable String ids) {
        Long[] idArr = Arrays.stream(ids.split(Constants.ENGLISH_COMMA))
                .map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).toArray(Long[]::new);
        return toAjax(jobLogService.deleteByIds(idArr));
    }
}
