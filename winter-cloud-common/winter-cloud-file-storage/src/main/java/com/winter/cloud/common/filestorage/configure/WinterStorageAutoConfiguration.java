package com.winter.cloud.common.filestorage.configure;

import com.winter.cloud.common.filestorage.StorageClient;
import com.winter.cloud.common.filestorage.StorageClientContext;
import com.winter.cloud.common.filestorage.clients.local.LocalStorageClient;
import com.winter.cloud.common.filestorage.clients.local.LocalStorageClientProperties;
import com.winter.cloud.common.filestorage.impl.StorageClientContextImpl;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import com.winter.cloud.common.filestorage.support.cache.LocalProxyCacheManager;
import com.winter.cloud.common.filestorage.support.cache.ProxyCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 存储核心自动装配（上下文 / 缓存 / 本地通道）。
 * 云厂商客户端见各 WinterStorage*Configuration，按 classpath 可选启用。
 */
@Configuration
@EnableConfigurationProperties({WinterStorageProperties.class})
public class WinterStorageAutoConfiguration {

    private final Logger logger = LoggerFactory.getLogger(WinterStorageAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(StorageClientContext.class)
    public StorageClientContext storageClientContext() {
        return new StorageClientContextImpl();
    }

    @Bean
    @ConditionalOnMissingBean(ProxyCacheManager.class)
    public ProxyCacheManager proxyCacheManager() {
        return new LocalProxyCacheManager();
    }

    @Bean(LocalStorageClientProperties.CHANNEL_BEAN_NAME)
    @ConditionalOnProperty(name = LocalStorageClientProperties.BEAN_CONDITIONAL_PROPERTY, havingValue = "true")
    @ConditionalOnMissingBean(LocalStorageClient.class)
    public StorageClient localStorageClient(WinterStorageProperties properties) {
        LocalStorageClientProperties clientProperties = properties.getLocal();
        clientProperties.initByProperties();
        return new LocalStorageClient(clientProperties);
    }

    @Bean
    public BeanPostProcessor cellStorageChannelBeanPostProcessor(StorageClientContext clientContext) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof StorageClient) {
                    StorageClient client = (StorageClient) bean;
                    logger.info(client.toString());
                    clientContext.register(client);
                }
                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                return bean;
            }
        };
    }
}
