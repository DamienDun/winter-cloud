package com.winter.cloud.common.filestorage.configure;

import com.winter.cloud.common.filestorage.StorageClient;
import com.winter.cloud.common.filestorage.clients.minio.MinioStorageClient;
import com.winter.cloud.common.filestorage.clients.minio.MinioStorageClientProperties;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "io.minio.MinioClient")
@AutoConfigureAfter(WinterStorageAutoConfiguration.class)
public class WinterStorageMinioConfiguration {

    @Bean(MinioStorageClientProperties.CHANNEL_BEAN_NAME)
    @ConditionalOnProperty(name = MinioStorageClientProperties.BEAN_CONDITIONAL_PROPERTY, havingValue = "true")
    @ConditionalOnMissingBean(MinioStorageClient.class)
    public StorageClient minioStorageClient(WinterStorageProperties properties) {
        return new MinioStorageClient(properties.getMinio());
    }
}