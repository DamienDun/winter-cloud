package com.winter.cloud.system;

import com.winter.cloud.common.security.annotation.EnableCustomConfig;
import com.winter.cloud.common.security.annotation.EnableRyFeignClients;
import com.winter.cloud.common.swagger.annotation.ApiGroup;
import com.winter.cloud.common.swagger.annotation.EnableWinterSwagger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 系统模块
 *
 * @author winter
 */
@EnableWinterSwagger(
        title = "WINTER微服务系统服务接口文档",
        description = "WINTER微服务系统服务接口文档",
        authorName = "WinterCloud",
        groups = {
                @ApiGroup(groupName = "系统模块", packages = {"com.winter.cloud.system.controller"}),
        })
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class WinterSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(WinterSystemApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  系统模块启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
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
