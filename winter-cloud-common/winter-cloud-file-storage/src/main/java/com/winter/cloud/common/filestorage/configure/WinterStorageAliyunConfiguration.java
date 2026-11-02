package com.winter.cloud.common.filestorage.configure;

import com.winter.cloud.common.filestorage.StorageClient;
import com.winter.cloud.common.filestorage.clients.aliyun.AliyunStorageClient;
import com.winter.cloud.common.filestorage.clients.aliyun.AliyunStorageClientProperties;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "com.aliyun.oss.OSS")
@AutoConfigureAfter(WinterStorageAutoConfiguration.class)
public class WinterStorageAliyunConfiguration {

    @Bean(AliyunStorageClientProperties.CHANNEL_BEAN_NAME)
    @ConditionalOnProperty(name = AliyunStorageClientProperties.BEAN_CONDITIONAL_PROPERTY, havingValue = "true")
    @ConditionalOnMissingBean(AliyunStorageClient.class)
    public StorageClient aliyunStorageClient(WinterStorageProperties properties) {
        return new AliyunStorageClient(properties.getAliyun());
    }
}