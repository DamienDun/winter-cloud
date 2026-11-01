package com.winter.cloud.common.filestorage.support.cache;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认本地缓存管理器
 */
public class LocalProxyCacheManager implements ProxyCacheManager {

    private final Map<String, ProxyCache> caches = new ConcurrentHashMap<>();

    @Override
    public ProxyCache getCache(String name) {
        return caches.computeIfAbsent(name, LocalProxyCache::new);
    }

    @Override
    public void clearCache(String name) {
        ProxyCache cache = caches.get(name);
        if (cache != null) {
            cache.clear();
        }
    }

    @Override
    public void clearCacheKey(String name, Object key) {
        ProxyCache cache = caches.get(name);
        if (cache != null) {
            cache.evict(key);
        }
    }

    @Override
    public void clearCacheKeys(String name, Object... keys) {
        ProxyCache cache = caches.get(name);
        if (cache != null) {
            cache.evict(keys);
        }
    }

    @Override
    public Collection<String> getCacheNames() {
        return caches.keySet();
    }
}
