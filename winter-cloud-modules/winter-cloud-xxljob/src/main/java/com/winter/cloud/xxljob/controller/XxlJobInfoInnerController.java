package com.winter.cloud.xxljob.controller;

import com.winter.cloud.common.core.web.controller.BaseController;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.security.annotation.InnerAuth;
import com.winter.cloud.xxljob.service.IXxlJobInfoService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Feign inner jobinfo APIs (from-source=inner) */
@Hidden
@RestController
@RequestMapping("/inner/jobinfo")
public class XxlJobInfoInnerController extends BaseController {

    @Autowired
    private IXxlJobInfoService jobInfoService;

    @InnerAuth
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> body) {
        return success(jobInfoService.add(body));
    }

    @InnerAuth
    @PutMapping
    public AjaxResult edit(@RequestBody Map<String, Object> body) {
        jobInfoService.update(body);
        return success();
    }

    @InnerAuth
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable("id") int id) {
        jobInfoService.remove(id);
        return success();
    }

    @InnerAuth
    @PutMapping("/start/{id}")
    public AjaxResult start(@PathVariable("id") int id) {
        jobInfoService.start(id);
        return success();
    }

    @InnerAuth
    @PutMapping("/stop/{id}")
    public AjaxResult stop(@PathVariable("id") int id) {
        jobInfoService.stop(id);
        return success();
    }

    @InnerAuth
    @PostMapping("/trigger")
    public AjaxResult trigger(@RequestBody Map<String, Object> body) {
        int id = Integer.parseInt(String.valueOf(body.get("id")));
        String executorParam = body.get("executorParam") == null ? "" : String.valueOf(body.get("executorParam"));
        String addressList = body.get("addressList") == null ? "" : String.valueOf(body.get("addressList"));
        jobInfoService.trigger(id, executorParam, addressList);
        return success();
    }

    @InnerAuth
    @GetMapping("/nextTriggerTime")
    public AjaxResult nextTriggerTime(@RequestParam("scheduleType") String scheduleType,
                                      @RequestParam("scheduleConf") String scheduleConf) {
        return success(jobInfoService.nextTriggerTime(scheduleType, scheduleConf));
    }
}
