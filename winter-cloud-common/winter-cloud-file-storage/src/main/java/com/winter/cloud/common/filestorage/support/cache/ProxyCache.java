package com.winter.cloud.common.filestorage.support.cache;

import org.springframework.lang.Nullable;

import java.util.concurrent.Callable;

/** 代理缓存 */
public interface ProxyCache {
    String getName();
    Object getNativeCache();
    Object get(Object key);
    <T> T get(Object key, @Nullable Class<T> type);
    <T> T get(Object key, Callable<T> valueLoader);
    void put(Object key, @Nullable Object value);
    void evict(Object key);
    void evict(Object... keys);
    void clear();
}