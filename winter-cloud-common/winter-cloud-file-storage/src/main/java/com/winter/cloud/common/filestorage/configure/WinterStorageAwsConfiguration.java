package com.winter.cloud.common.filestorage.configure;

import com.winter.cloud.common.filestorage.StorageClient;
import com.winter.cloud.common.filestorage.clients.aws.AwsStorageClient;
import com.winter.cloud.common.filestorage.clients.aws.AwsStorageClientProperties;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "com.amazonaws.services.s3.AmazonS3")
@AutoConfigureAfter(WinterStorageAutoConfiguration.class)
public class WinterStorageAwsConfiguration {

    @Bean(AwsStorageClientProperties.CHANNEL_BEAN_NAME)
    @ConditionalOnProperty(name = AwsStorageClientProperties.BEAN_CONDITIONAL_PROPERTY, havingValue = "true")
    @ConditionalOnMissingBean(AwsStorageClient.class)
    public StorageClient awsStorageClient(WinterStorageProperties properties) {
        return new AwsStorageClient(properties.getAws());
    }
}