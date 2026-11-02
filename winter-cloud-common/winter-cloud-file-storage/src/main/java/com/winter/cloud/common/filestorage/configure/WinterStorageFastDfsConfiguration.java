package com.winter.cloud.common.filestorage.configure;

import com.winter.cloud.common.filestorage.StorageClient;
import com.winter.cloud.common.filestorage.clients.fastdfs.FastDFSStorageClient;
import com.winter.cloud.common.filestorage.clients.fastdfs.FastDFSStorageClientProperties;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "org.csource.fastdfs.ClientGlobal")
@AutoConfigureAfter(WinterStorageAutoConfiguration.class)
public class WinterStorageFastDfsConfiguration {

    @Bean(FastDFSStorageClientProperties.CHANNEL_BEAN_NAME)
    @ConditionalOnProperty(name = FastDFSStorageClientProperties.BEAN_CONDITIONAL_PROPERTY, havingValue = "true")
    @ConditionalOnMissingBean(FastDFSStorageClient.class)
    public StorageClient fastDFSStorageClient(WinterStorageProperties properties) {
        return new FastDFSStorageClient(properties.getFastDFS());
    }
}