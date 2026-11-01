package com.winter.cloud.common.filestorage.support.cache;

import java.util.Collection;

/** 代理缓存管理 */
public interface ProxyCacheManager {
    ProxyCache getCache(String name);
    void clearCache(String name);
    void clearCacheKey(String name, Object key);
    void clearCacheKeys(String name, Object... keys);
    Collection<String> getCacheNames();
}