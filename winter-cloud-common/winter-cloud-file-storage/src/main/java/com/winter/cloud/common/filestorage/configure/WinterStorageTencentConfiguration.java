package com.winter.cloud.common.filestorage.configure;

import com.winter.cloud.common.filestorage.StorageClient;
import com.winter.cloud.common.filestorage.clients.tencent.TencentStorageClient;
import com.winter.cloud.common.filestorage.clients.tencent.TencentStorageClientProperties;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "com.qcloud.cos.COSClient")
@AutoConfigureAfter(WinterStorageAutoConfiguration.class)
public class WinterStorageTencentConfiguration {

    @Bean(TencentStorageClientProperties.CHANNEL_BEAN_NAME)
    @ConditionalOnProperty(name = TencentStorageClientProperties.BEAN_CONDITIONAL_PROPERTY, havingValue = "true")
    @ConditionalOnMissingBean(TencentStorageClient.class)
    public StorageClient tencentStorageClient(WinterStorageProperties properties) {
        return new TencentStorageClient(properties.getTencent());
    }
}