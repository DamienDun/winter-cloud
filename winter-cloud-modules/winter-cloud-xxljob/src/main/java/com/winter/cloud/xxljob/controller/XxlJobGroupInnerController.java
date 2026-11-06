package com.winter.cloud.xxljob.controller;

import com.winter.cloud.common.core.web.controller.BaseController;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.security.annotation.InnerAuth;
import com.winter.cloud.xxljob.service.IXxlJobGroupService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Feign inner jobgroup APIs (from-source=inner) */
@Hidden
@RestController
@RequestMapping("/inner/jobgroup")
public class XxlJobGroupInnerController extends BaseController {

    @Autowired
    private IXxlJobGroupService jobGroupService;

    @InnerAuth
    @GetMapping("/all")
    public AjaxResult all() {
        return success(jobGroupService.listAll());
    }

    @InnerAuth
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> body) {
        jobGroupService.save(body);
        return success();
    }
}
