package com.winter.cloud.common.filestorage.support.lock;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 基于 Redisson 的分布式锁
 */
@Slf4j
public class DistributionLockCli {

    private static final long DEFAULT_WAIT_SECONDS = 1L;

    private final RedissonClient redissonClient;

    public DistributionLockCli(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public String lock(String key) throws Exception {
        return lock(key, DEFAULT_WAIT_SECONDS, TimeUnit.SECONDS);
    }

    public String lock(String key, long waitTime, TimeUnit unit) throws Exception {
        RLock lock = redissonClient.getLock(key);
        boolean acquired = lock.tryLock(waitTime, unit);
        if (acquired) {
            log.info("获取分布式锁[key={}]", key);
            return key;
        }
        return null;
    }

    public void lock(String key, Consumer<String> lockUnitOfWorker) {
        String lockId = null;
        try {
            lockId = lock(key);
            if (lockId == null) {
                log.warn("获取分布式锁失败[key={}]", key);
                return;
            }
            if (lockUnitOfWorker != null) {
                lockUnitOfWorker.accept(key);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (lockId != null) {
                unlock(key);
            }
        }
    }

    public void unlock(String key) {
        RLock lock = redissonClient.getLock(key);
        if (!lock.isHeldByCurrentThread()) {
            return;
        }
        try {
            lock.unlock();
            log.info("释放分布式锁[key={}]", key);
        } catch (Exception ex) {
            log.warn("释放分布式锁异常[key={}]", key, ex);
        }
    }
}
