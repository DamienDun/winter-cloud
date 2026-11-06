package com.winter.cloud.datasource.controller;

import com.winter.cloud.common.core.constant.Constants;
import com.winter.cloud.common.core.web.controller.BaseController;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.core.web.page.TableDataInfo;
import com.winter.cloud.common.log.annotation.Log;
import com.winter.cloud.common.log.enums.BusinessType;
import com.winter.cloud.common.security.annotation.RequiresPermissions;
import com.winter.cloud.datasource.domain.entity.DsJobInfo;
import com.winter.cloud.datasource.service.IDsJobInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 作业信息（调度由 winter-xxljob 承担，本地保存 xxlJobId）
 */
@RestController
@RequestMapping("/job")
@Tag(name = "作业信息")
public class DsJobInfoController extends BaseController {

    @Autowired
    private IDsJobInfoService jobInfoService;

    @RequiresPermissions("datasource:job:list")
    @GetMapping("/list")
    @Operation(summary = "分页查询")
    public TableDataInfo list(DsJobInfo query) {
        startPage();
        List<DsJobInfo> list = jobInfoService.selectList(query);
        return getDataTable(list);
    }

    @RequiresPermissions("datasource:job:query")
    @GetMapping("/{id}")
    @Operation(summary = "详情")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(jobInfoService.selectById(id));
    }

    @RequiresPermissions("datasource:job:add")
    @Log(title = "作业信息", businessType = BusinessType.INSERT)
    @PostMapping
    @Operation(summary = "新增（同步创建XXL-JOB任务）")
    public AjaxResult add(@RequestBody DsJobInfo entity) {
        try {
            return toAjax(jobInfoService.insert(entity));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:job:edit")
    @Log(title = "作业信息", businessType = BusinessType.UPDATE)
    @PutMapping
    @Operation(summary = "修改")
    public AjaxResult edit(@RequestBody DsJobInfo entity) {
        try {
            return toAjax(jobInfoService.update(entity));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:job:remove")
    @Log(title = "作业信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    @Operation(summary = "删除")
    public AjaxResult remove(@PathVariable String ids) {
        try {
            Long[] idArr = Arrays.stream(ids.split(Constants.ENGLISH_COMMA))
                    .map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).toArray(Long[]::new);
            return toAjax(jobInfoService.deleteByIds(idArr));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:job:changeStatus")
    @Log(title = "作业信息", businessType = BusinessType.UPDATE)
    @PutMapping("/start/{id}")
    @Operation(summary = "开启作业")
    public AjaxResult start(@PathVariable Long id) {
        try {
            jobInfoService.start(id);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:job:changeStatus")
    @Log(title = "作业信息", businessType = BusinessType.UPDATE)
    @PutMapping("/stop/{id}")
    @Operation(summary = "停用作业")
    public AjaxResult stop(@PathVariable Long id) {
        try {
            jobInfoService.stop(id);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:job:execute")
    @Log(title = "作业信息", businessType = BusinessType.OTHER)
    @PostMapping("/execute/{id}")
    @Operation(summary = "手动执行一次")
    public AjaxResult execute(@PathVariable Long id) {
        try {
            jobInfoService.execute(id);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("datasource:job:list")
    @GetMapping("/nextTriggerTime")
    @Operation(summary = "根据Cron计算下次执行时间")
    public AjaxResult nextTriggerTime(@RequestParam String cron) {
        try {
            return success(jobInfoService.nextTriggerTime(cron));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
