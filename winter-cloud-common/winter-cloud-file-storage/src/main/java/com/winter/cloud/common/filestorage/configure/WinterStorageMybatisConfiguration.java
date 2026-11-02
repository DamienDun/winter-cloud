package com.winter.cloud.common.filestorage.configure;

import com.winter.cloud.common.filestorage.StorageClientContext;
import com.winter.cloud.common.filestorage.mapper.FsBigFileUploadTaskMapper;
import com.winter.cloud.common.filestorage.service.BigFileUploadService;
import com.winter.cloud.common.filestorage.service.FsBigFileUploadTaskService;
import com.winter.cloud.common.filestorage.service.impl.FsBigFileUploadTaskServiceImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Big-file upload task persistence (requires MyBatis).
 */
@Configuration
@ConditionalOnClass({SqlSessionFactory.class, MapperScan.class})
@MapperScan("com.winter.cloud.common.filestorage.mapper")
public class WinterStorageMybatisConfiguration {

    /**
     * Do not use @ConditionalOnBean(FsBigFileUploadTaskMapper):
     * Mapper is registered by @MapperScan on this class and is not visible
     * during condition evaluation, so the Service would never be created.
     * BigFileUploadService / StorageClientContext 用 ObjectProvider，避免与分片服务构造环依赖。
     */
    @Bean
    @ConditionalOnMissingBean(FsBigFileUploadTaskService.class)
    public FsBigFileUploadTaskService fsBigFileUploadTaskService(FsBigFileUploadTaskMapper mapper,
                                                                ObjectProvider<BigFileUploadService> bigFileUploadService,
                                                                ObjectProvider<StorageClientContext> storageClientContext) {
        return new FsBigFileUploadTaskServiceImpl(mapper, bigFileUploadService, storageClientContext);
    }
}