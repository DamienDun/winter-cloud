package com.winter.cloud.auth;

import com.winter.cloud.common.security.annotation.EnableRyFeignClients;
import com.winter.cloud.common.swagger.annotation.ApiGroup;
import com.winter.cloud.common.swagger.annotation.EnableWinterSwagger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 认证授权中心
 *
 * @author winter
 */
@EnableWinterSwagger(
        title = "WINTER微服务认证授权接口文档",
        description = "WINTER微服务认证授权接口文档",
        authorName = "WinterCloud",
        groups = {
                @ApiGroup(groupName = "认证授权模块", packages = {"com.winter.cloud.auth.controller"}),
        })
@EnableRyFeignClients
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class WinterAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(WinterAuthApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  认证授权中心启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " .-------.       ____     __        \n" +
                " |  _ _   \\      \\   \\   /  /    \n" +
                " | ( ' )  |       \\  _. /  '       \n" +
                " |(_ o _) /        _( )_ .'         \n" +
                " | (_,_).' __  ___(_ o _)'          \n" +
                " |  |\\ \\  |  ||   |(_,_)'         \n" +
                " |  | \\ `'   /|   `-'  /           \n" +
                " |  |  \\    /  \\      /           \n" +
                " ''-'   `'-'    `-..-'              ");
    }
}
