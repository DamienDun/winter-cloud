package com.winter.cloud.common.filestorage.configure;

import com.winter.cloud.common.filestorage.StorageClientContext;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import com.winter.cloud.common.filestorage.service.BigFileUploadService;
import com.winter.cloud.common.filestorage.service.FsBigFileUploadTaskService;
import com.winter.cloud.common.filestorage.service.impl.BigFileUploadServiceImpl;
import com.winter.cloud.common.filestorage.support.cache.ProxyCacheManager;
import com.winter.cloud.common.filestorage.support.lock.DistributionLockCli;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Big-file multipart upload auto-config.
 * Must run after storage client / Redisson lock / task mapper,
 * otherwise @ConditionalOnBean evaluates too early and Swagger misses the APIs.
 */
@Configuration
@AutoConfigureAfter({
        WinterStorageAutoConfiguration.class,
        WinterStorageLockConfiguration.class,
        WinterStorageMybatisConfiguration.class
})
@ConditionalOnBean({
        StorageClientContext.class,
        DistributionLockCli.class,
        ProxyCacheManager.class,
        FsBigFileUploadTaskService.class
})
public class WinterStorageBigFileConfiguration {

    @Bean
    @ConditionalOnMissingBean(BigFileUploadService.class)
    public BigFileUploadService bigFileUploadService(StorageClientContext storageClientContext,
                                                     DistributionLockCli distributionLockCli,
                                                     ProxyCacheManager proxyCacheManager,
                                                     FsBigFileUploadTaskService fsBigFileUploadTaskService,
                                                     WinterStorageProperties properties) {
        return new BigFileUploadServiceImpl(storageClientContext, distributionLockCli,
                proxyCacheManager, fsBigFileUploadTaskService, properties);
    }
}