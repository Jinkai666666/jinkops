package com.jinkops.cache.service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
@Service
public class CacheService {
    private final StringRedisTemplate redis ;

    public CacheService(StringRedisTemplate redis) {
        this.redis   = redis;
    }


    //写入缓存
    public void set(String key, String value , long ttlSeconds) {
        redis.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    //读取缓存
    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    //删除缓存
    public void delete(String key) {
        redis.delete(key);
    }

    // 判断是否存在
    public Boolean exists(String key) {
        return redis.hasKey(key);
    }

    // 设置过期时间
    public void expire(String key, long ttlSeconds) {
        redis.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }

    // 查询剩余 TTL
    public Long ttl(String key) {
        return redis.getExpire(key);
    }
}
