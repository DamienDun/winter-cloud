package com.winter.cloud.xxljob.controller;

import com.winter.cloud.common.core.web.controller.BaseController;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.core.web.page.TableDataInfo;
import com.winter.cloud.common.log.annotation.Log;
import com.winter.cloud.common.log.enums.BusinessType;
import com.winter.cloud.common.security.annotation.RequiresPermissions;
import com.winter.cloud.xxljob.service.IXxlJobGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/jobgroup")
@Tag(name = "XXL-JOB Executor Group")
public class XxlJobGroupController extends BaseController {

    @Autowired
    private IXxlJobGroupService jobGroupService;

    @RequiresPermissions("xxljob:jobgroup:list")
    @GetMapping("/list")
    @Operation(summary = "page list")
    public TableDataInfo list(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              String appname, String title) {
        try {
            Map<String, Object> page = jobGroupService.pageList(pageNum, pageSize, appname, title);
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

    @RequiresPermissions("xxljob:jobgroup:list")
    @GetMapping("/all")
    @Operation(summary = "all groups")
    public AjaxResult all() {
        try {
            return success(jobGroupService.listAll());
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("xxljob:jobgroup:add")
    @Log(title = "XXL-JOB Group", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> body) {
        try {
            jobGroupService.save(body);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("xxljob:jobgroup:edit")
    @Log(title = "XXL-JOB Group", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Map<String, Object> body) {
        try {
            jobGroupService.update(body);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("xxljob:jobgroup:remove")
    @Log(title = "XXL-JOB Group", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable int id) {
        try {
            jobGroupService.remove(id);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
