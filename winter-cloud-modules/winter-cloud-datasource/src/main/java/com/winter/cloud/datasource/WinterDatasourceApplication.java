package com.winter.cloud.datasource;

import com.winter.cloud.common.security.annotation.EnableRyFeignClients;
import com.winter.cloud.common.security.config.ApplicationConfig;
import com.winter.cloud.common.security.feign.FeignAutoConfiguration;
import com.winter.cloud.common.swagger.annotation.ApiGroup;
import com.winter.cloud.common.swagger.annotation.EnableWinterSwagger;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 数据源管理服务
 * <p>
 * 不用 EnableCustomConfig 的 {@code com.winter.**.mapper} 宽扫描，
 * 否则会把 common-mybatis 里的泛型 {@code DefaultBaseMapper} 误注册成 Bean 导致启动失败
 * （与 winter-file 做法一致）。
 */
@EnableWinterSwagger(
        title = "WINTER微服务数据源管理接口文档",
        description = "数据源维护与连接测试",
        authorName = "WinterCloud",
        groups = {
                @ApiGroup(groupName = "数据源管理模块", packages = {"com.winter.cloud.datasource.controller"}),
                @ApiGroup(groupName = "作业管理模块", packages = {"com.winter.cloud.datasource.controller"}),
        })
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync
@Import({ApplicationConfig.class, FeignAutoConfiguration.class})
@MapperScan("com.winter.cloud.datasource.mapper")
@EnableRyFeignClients
@SpringBootApplication
public class WinterDatasourceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WinterDatasourceApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  数据源管理模块启动成功   ლ(´ڡ`ლ)ﾞ");
    }
}
