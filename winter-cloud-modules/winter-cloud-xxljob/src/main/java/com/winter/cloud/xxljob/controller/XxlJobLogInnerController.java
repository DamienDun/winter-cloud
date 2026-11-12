package com.winter.cloud.xxljob.controller;

import com.winter.cloud.common.core.web.controller.BaseController;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.security.annotation.InnerAuth;
import com.winter.cloud.xxljob.service.IXxlJobLogService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Feign inner joblog APIs (from-source=inner) */
@Hidden
@RestController
@RequestMapping("/inner/joblog")
public class XxlJobLogInnerController extends BaseController {

    @Autowired
    private IXxlJobLogService jobLogService;

    @InnerAuth
    @PostMapping("/kill/{id}")
    public AjaxResult kill(@PathVariable("id") int id) {
        jobLogService.kill(id);
        return success();
    }
}
