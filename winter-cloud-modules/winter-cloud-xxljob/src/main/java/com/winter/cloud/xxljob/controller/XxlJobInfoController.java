package com.winter.cloud.xxljob.controller;

import com.winter.cloud.common.core.web.controller.BaseController;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.core.web.page.TableDataInfo;
import com.winter.cloud.common.log.annotation.Log;
import com.winter.cloud.common.log.enums.BusinessType;
import com.winter.cloud.common.security.annotation.RequiresPermissions;
import com.winter.cloud.xxljob.service.IXxlJobInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/jobinfo")
@Tag(name = "XXL-JOB Job Info")
public class XxlJobInfoController extends BaseController {

    @Autowired
    private IXxlJobInfoService jobInfoService;

    @RequiresPermissions("xxljob:jobinfo:list")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              Integer jobGroup, Integer triggerStatus,
                              String jobDesc, String executorHandler, String author) {
        try {
            Map<String, Object> page = jobInfoService.pageList(pageNum, pageSize, jobGroup, triggerStatus, jobDesc, executorHandler, author);
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

    @RequiresPermissions("xxljob:jobinfo:list")
    @GetMapping("/enums")
    @Operation(summary = "dropdown enums")
    public AjaxResult enums() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("scheduleType", new String[]{"NONE", "CRON", "FIX_RATE"});
        map.put("misfireStrategy", new String[]{"DO_NOTHING", "FIRE_ONCE_NOW"});
        map.put("glueType", new String[]{"BEAN", "GLUE_GROOVY", "GLUE_SHELL", "GLUE_PYTHON", "GLUE_PHP", "GLUE_NODEJS", "GLUE_POWERSHELL"});
        map.put("executorRouteStrategy", new String[]{"FIRST", "LAST", "ROUND", "RANDOM", "CONSISTENT_HASH", "LEAST_FREQUENTLY_USED", "LEAST_RECENTLY_USED", "FAILOVER", "BUSYOVER", "SHARDING_BROADCAST"});
        map.put("executorBlockStrategy", new String[]{"SERIAL_EXECUTION", "DISCARD_LATER", "COVER_EARLY"});
        return success(map);
    }

    @RequiresPermissions("xxljob:jobinfo:add")
    @Log(title = "XXL-JOB Info", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> body) {
        try {
            jobInfoService.add(body);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("xxljob:jobinfo:edit")
    @Log(title = "XXL-JOB Info", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Map<String, Object> body) {
        try {
            jobInfoService.update(body);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("xxljob:jobinfo:remove")
    @Log(title = "XXL-JOB Info", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable int id) {
        try {
            jobInfoService.remove(id);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("xxljob:jobinfo:edit")
    @Log(title = "XXL-JOB Info", businessType = BusinessType.UPDATE)
    @PutMapping("/start/{id}")
    public AjaxResult start(@PathVariable int id) {
        try {
            jobInfoService.start(id);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("xxljob:jobinfo:edit")
    @Log(title = "XXL-JOB Info", businessType = BusinessType.UPDATE)
    @PutMapping("/stop/{id}")
    public AjaxResult stop(@PathVariable int id) {
        try {
            jobInfoService.stop(id);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("xxljob:jobinfo:trigger")
    @Log(title = "XXL-JOB Info", businessType = BusinessType.OTHER)
    @PostMapping("/trigger")
    public AjaxResult trigger(@RequestBody Map<String, Object> body) {
        try {
            int id = Integer.parseInt(String.valueOf(body.get("id")));
            String executorParam = body.get("executorParam") == null ? "" : String.valueOf(body.get("executorParam"));
            String addressList = body.get("addressList") == null ? "" : String.valueOf(body.get("addressList"));
            jobInfoService.trigger(id, executorParam, addressList);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("xxljob:jobinfo:list")
    @GetMapping("/nextTriggerTime")
    public AjaxResult nextTriggerTime(String scheduleType, String scheduleConf) {
        try {
            return success(jobInfoService.nextTriggerTime(scheduleType, scheduleConf));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("xxljob:jobinfo:list")
    @GetMapping("/{id}")
    @Operation(summary = "任务详情（含 GLUE 历史）")
    public AjaxResult detail(@PathVariable int id) {
        try {
            return success(jobInfoService.loadDetail(id));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @RequiresPermissions("xxljob:jobinfo:edit")
    @Log(title = "XXL-JOB GLUE", businessType = BusinessType.UPDATE)
    @PostMapping("/glue")
    @Operation(summary = "保存 GLUE 源码")
    public AjaxResult saveGlue(@RequestBody Map<String, Object> body) {
        try {
            int id = Integer.parseInt(String.valueOf(body.get("id")));
            String glueSource = body.get("glueSource") == null ? "" : String.valueOf(body.get("glueSource"));
            String glueRemark = body.get("glueRemark") == null ? "" : String.valueOf(body.get("glueRemark"));
            jobInfoService.saveGlue(id, glueSource, glueRemark);
            return success();
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
