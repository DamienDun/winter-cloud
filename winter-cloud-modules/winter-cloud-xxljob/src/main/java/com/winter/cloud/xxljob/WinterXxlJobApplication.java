package com.winter.cloud.xxljob;

import com.winter.cloud.common.security.annotation.EnableCustomConfig;
import com.winter.cloud.common.security.annotation.EnableRyFeignClients;
import com.winter.cloud.common.swagger.annotation.ApiGroup;
import com.winter.cloud.common.swagger.annotation.EnableWinterSwagger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Embedded XXL-JOB admin (2.3.0 scheduler + Winter REST API)
 */
@EnableWinterSwagger(
        title = "WINTER微服务XXL-JOB接口文档",
        description = "内嵌 XXL-JOB 调度中心（任务/日志/执行器/报表）",
        authorName = "WinterCloud",
        groups = {
                @ApiGroup(groupName = "XXL-JOB管理模块", packages = {"com.winter.cloud.xxljob.controller"}),
        })
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class WinterXxlJobApplication {
    public static void main(String[] args) {
        SpringApplication.run(WinterXxlJobApplication.class, args);
        System.out.println("(ok) winter-xxljob started (embedded xxl-job-admin 2.3.0)");
    }
}
