package com.winter.cloud.common.filestorage.configure;

import com.winter.cloud.common.filestorage.support.lock.DistributionLockCli;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 分布式锁配置（仅在存在 Redisson 时生效）。
 */
@Configuration
@ConditionalOnClass(name = "org.redisson.api.RedissonClient")
public class WinterStorageLockConfiguration {

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(DistributionLockCli.class)
    public DistributionLockCli distributionLockCli(RedissonClient redissonClient) {
        return new DistributionLockCli(redissonClient);
    }
}
