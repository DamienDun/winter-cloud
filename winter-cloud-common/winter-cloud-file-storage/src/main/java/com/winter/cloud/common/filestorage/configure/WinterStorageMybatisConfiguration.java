package com.winter.cloud.common.filestorage.configure;

import com.winter.cloud.common.filestorage.mapper.FsBigFileUploadTaskMapper;
import com.winter.cloud.common.filestorage.service.FsBigFileUploadTaskService;
import com.winter.cloud.common.filestorage.service.impl.FsBigFileUploadTaskServiceImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 大文件上传任务持久化配置（仅在存在 MyBatis 时生效）。
 */
@Configuration
@ConditionalOnClass({SqlSessionFactory.class, MapperScan.class})
@MapperScan("com.winter.cloud.common.filestorage.mapper")
public class WinterStorageMybatisConfiguration {

    @Bean
    @ConditionalOnBean(FsBigFileUploadTaskMapper.class)
    @ConditionalOnMissingBean(FsBigFileUploadTaskService.class)
    public FsBigFileUploadTaskService fsBigFileUploadTaskService(FsBigFileUploadTaskMapper mapper) {
        return new FsBigFileUploadTaskServiceImpl(mapper);
    }
}
