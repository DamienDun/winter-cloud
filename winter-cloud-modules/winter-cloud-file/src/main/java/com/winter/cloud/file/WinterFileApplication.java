package com.winter.cloud.file;

import com.winter.cloud.common.core.config.WinterConfig;
import com.winter.cloud.common.filestorage.annotation.EnableWinterStorage;
import com.winter.cloud.common.security.annotation.EnableRyFeignClients;
import com.winter.cloud.common.security.feign.FeignAutoConfiguration;
import com.winter.cloud.common.swagger.annotation.ApiGroup;
import com.winter.cloud.common.swagger.annotation.EnableWinterSwagger;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

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
@EnableWinterStorage
@EnableRyFeignClients
// 不启用 EnableCustomConfig：其 MapperScan(com.winter.**.mapper) 会误扫 file-storage 泛型基座
@Import({WinterConfig.class, FeignAutoConfiguration.class})
// 仅扫描本模块；file-storage 的 Mapper 由 WinterStorageMybatisConfiguration 单独扫描，
// 避免把 support.mapper.StorageBaseMapper 这类泛型基座误注册成 Bean
@MapperScan("com.winter.cloud.file.mapper")
@SpringBootApplication
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