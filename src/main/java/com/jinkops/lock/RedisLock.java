package com.jinkops.lock;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;


////Redis 分布式锁组件
@Component
public class RedisLock {

    private final StringRedisTemplate redis;

    public RedisLock(StringRedisTemplate redis) {
        this.redis = redis;
    }
    /**
     * 尝试获取分布式锁
     * @param key 锁名
     * @param ttlSeconds 过期时间（秒）
     * @return 加锁成功返回 value，失败返回 null
     */
    public String tryLock(String key, long ttlSeconds) {

        // 唯一标识，用于释放锁时校验身份
        String value = java.util.UUID.randomUUID().toString();

        // SETNX + EXPIRE
        Boolean ok = redis.opsForValue()
                .setIfAbsent(key, value, ttlSeconds, java.util.concurrent.TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(ok)) {
            System.out.println("lock ok: key=" + key + " value=" + value);
            return value;
        }

        System.out.println("lock fail: key=" + key);
        return null;
    }




    /**
     * 释放分布式锁
     * @param key 锁名
     * @param value 加锁时返回的 UUID，用于身份校验
     * @return 是否释放成功
     */
    public boolean unlock(String key, String value) {

        // 当前锁的 value，用来判断锁是否仍然属于自己
        String current = redis.opsForValue().get(key);

        // 只有 value 匹配时才能释放锁
        if (value != null && value.equals(current)) {
            redis.delete(key);
            System.out.println("unlock ok: key=" + key);
            return true;
        }

        // 不匹配,锁已过期或使用中，不能删
        System.out.println("unlock skip: key=" + key);
        return false;
    }



}
