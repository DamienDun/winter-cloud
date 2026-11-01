package com.winter.cloud.common.filestorage.support.cache;

import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/** 本地内存缓存实现 */
public class LocalProxyCache implements ProxyCache {

    private final String name;
    private final Map<Object, Object> store = new ConcurrentHashMap<>();

    public LocalProxyCache(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return store;
    }

    @Override
    public Object get(Object key) {
        return store.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, @Nullable Class<T> type) {
        Object value = store.get(key);
        if (value == null) {
            return null;
        }
        return (T) value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object value = store.get(key);
        if (value != null) {
            return (T) value;
        }
        try {
            T loaded = valueLoader.call();
            if (loaded != null) {
                store.put(key, loaded);
            }
            return loaded;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void put(Object key, @Nullable Object value) {
        if (value == null) {
            store.remove(key);
        } else {
            store.put(key, value);
        }
    }

    @Override
    public void evict(Object key) {
        store.remove(key);
    }

    @Override
    public void evict(Object... keys) {
        if (keys == null) {
            return;
        }
        for (Object key : keys) {
            store.remove(key);
        }
    }

    @Override
    public void clear() {
        store.clear();
    }
}