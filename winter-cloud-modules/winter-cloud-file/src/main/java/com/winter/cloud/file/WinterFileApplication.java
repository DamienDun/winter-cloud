package com.winter.cloud.file;

import com.winter.cloud.common.swagger.annotation.ApiGroup;
import com.winter.cloud.common.swagger.annotation.EnableWinterSwagger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 文件服务
 *
 * @author winter
 */
@EnableWinterSwagger(
        title = "WINTER微服务文件服务接口文档",
        description = "WINTER微服务文件服务接口文档",
        authorName = "WinterCloud",
        groups = {
                @ApiGroup(groupName = "文件服务模块", packages = {"com.winter.cloud.file.controller"}),
        })
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class WinterFileApplication {
    public static void main(String[] args) {
        SpringApplication.run(WinterFileApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  文件服务模块启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
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
