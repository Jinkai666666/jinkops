package com.jinkops.cache.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {
    private final StringRedisTemplate redis;

    public CacheService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    // 寫入快取
    public void set(String key, String value, long ttlSeconds) {
        redis.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    // 讀取快取
    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    // 刪除快取
    public void delete(String key) {
        redis.delete(key);
    }

    // 判斷是否存在
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

    // 查詢剩餘 TTL（秒）
    public long ttl(String key) {
        Long expire = redis.getExpire(key, TimeUnit.SECONDS);
        return expire == null ? -2 : expire; // 防止返回 null
    }

    // 刪除分頁快取
    public void deleteByPrefix(String prefix) {
        Set<String> keys = redis.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }
}
