package com.winter.cloud.common.filestorage.configure;

import com.winter.cloud.common.filestorage.StorageClient;
import com.winter.cloud.common.filestorage.clients.huawei.HuaWeiStorageClient;
import com.winter.cloud.common.filestorage.clients.huawei.HuaWeiStorageClientProperties;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "com.obs.services.ObsClient")
@AutoConfigureAfter(WinterStorageAutoConfiguration.class)
public class WinterStorageHuaWeiConfiguration {

    @Bean(HuaWeiStorageClientProperties.CHANNEL_BEAN_NAME)
    @ConditionalOnProperty(name = HuaWeiStorageClientProperties.BEAN_CONDITIONAL_PROPERTY, havingValue = "true")
    @ConditionalOnMissingBean(HuaWeiStorageClient.class)
    public StorageClient huaWeiStorageClient(WinterStorageProperties properties) {
        return new HuaWeiStorageClient(properties.getHuawei());
    }
}