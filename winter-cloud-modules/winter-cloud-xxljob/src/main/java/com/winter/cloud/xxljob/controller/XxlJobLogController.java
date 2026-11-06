package com.winter.cloud.xxljob.controller;

import com.winter.cloud.common.core.web.controller.BaseController;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.core.web.page.TableDataInfo;
import com.winter.cloud.common.log.annotation.Log;
import com.winter.cloud.common.log.enums.BusinessType;
import com.winter.cloud.common.security.annotation.RequiresPermissions;
import com.winter.cloud.xxljob.service.IXxlJobLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/joblog")
@Tag(name = "XXL-JOB调度日志")
public class XxlJobLogController extends BaseController {

    @Autowired
    private IXxlJobLogService jobLogService;

    @RequiresPermissions("xxljob:joblog:list")
    @GetMapping("/list")
    @Operation(summary = "调度日志分页")
    public TableDataInfo list(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              Integer jobGroup, Integer jobId, Integer logStatus, String filterTime) {
        try {
            Map<String, Object> page = jobLogService.pageList(pageNum, pageSize, jobGroup, jobId, logStatus, filterTime);
            TableDataInfo rsp = new TableDataInfo();
            rsp.setCode(200);
            rsp.setRows((java.util.List<?>) page.get("rows"));
            Object total = page.get("total");
            rsp.setTotal(total == null ? 0L : Long.parseLong(String.valueOf(total)));
            return rsp;
        } catch (Exception e) {
            TableDataInfo rsp = new TableDataInfo();
            rsp.setCode(500);
            rsp.setMsg(e.getMessage());
            return rsp;
        }
    }

    @RequiresPermissions("xxljob:joblog:list")
    @GetMapping("/jobs")
    @Operation(summary = "按执行器查任务下拉")
    public AjaxResult jobs(@RequestParam int jobGroup) {
        try {
            return success(jobLogService.getJobsByGroup(jobGroup));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("xxljob:joblog:list")
    @GetMapping("/detail")
    @Operation(summary = "查看执行日志（滚动）")
    public AjaxResult detail(String executorAddress, Long triggerTime, Long logId,
                             @RequestParam(defaultValue = "1") Integer fromLineNum) {
        try {
            return success(jobLogService.logDetailCat(executorAddress, triggerTime, logId, fromLineNum));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("xxljob:joblog:remove")
    @Log(title = "XXL-JOB Log", businessType = BusinessType.CLEAN)
    @DeleteMapping("/clear")
    @Operation(summary = "清理调度日志")
    public AjaxResult clear(@RequestParam int jobGroup, @RequestParam int jobId, @RequestParam int type) {
        try {
            jobLogService.clearLog(jobGroup, jobId, type);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("xxljob:joblog:remove")
    @Log(title = "XXL-JOB Log", businessType = BusinessType.OTHER)
    @PostMapping("/kill/{id}")
    @Operation(summary = "终止任务执行")
    public AjaxResult kill(@PathVariable int id) {
        try {
            jobLogService.kill(id);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
