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
        title = "WINTER XXL-JOB API",
        description = "Embedded XXL-JOB admin APIs",
        authorName = "WinterCloud",
        groups = {
                @ApiGroup(groupName = "XXL-JOB", packages = {"com.winter.cloud.xxljob.controller"}),
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