package com.winter.cloud.common.dingtalk.support;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Local cache for DingTalk accessToken / jsapiTicket.
 */
public class LocalCacheUtil {

    private static final ConcurrentHashMap<String, LocalCacheData> CACHE_MAP = new ConcurrentHashMap<>();

    public static boolean set(String key, Object val, long cacheTime) {
        cleanExpireCache();
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        if (val == null || cacheTime < 0) {
            CACHE_MAP.remove(key);
            return true;
        }
        CACHE_MAP.put(key, new LocalCacheData(key, val, System.currentTimeMillis() + cacheTime));
        return true;
    }

    public static Object get(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        LocalCacheData localCacheData = CACHE_MAP.get(key);
        if (localCacheData != null && System.currentTimeMillis() < localCacheData.getExpireTime()) {
            return localCacheData.getVal();
        }
        CACHE_MAP.remove(key);
        return null;
    }

    public static boolean remove(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        CACHE_MAP.remove(key);
        return true;
    }

    public static void cleanExpireCache() {
        if (CACHE_MAP.isEmpty()) {
            return;
        }
        for (String key : CACHE_MAP.keySet()) {
            LocalCacheData localCacheData = CACHE_MAP.get(key);
            if (localCacheData != null && System.currentTimeMillis() >= localCacheData.getExpireTime()) {
                CACHE_MAP.remove(key);
            }
        }
    }

    private static class LocalCacheData {
        private final Object val;
        private final long expireTime;

        public LocalCacheData(String key, Object val, long expireTime) {
            this.val = val;
            this.expireTime = expireTime;
        }

        public Object getVal() {
            return val;
        }

        public long getExpireTime() {
            return expireTime;
        }
    }
}
