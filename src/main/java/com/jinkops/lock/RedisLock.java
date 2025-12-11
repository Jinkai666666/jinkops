package com.jinkops.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;


////Redis 分佈式鎖組件
@Component
@RequiredArgsConstructor
public class RedisLock {

    private final StringRedisTemplate redis;
    /**
     * 嘗試獲取分佈式鎖
     * @param key 鎖名
     * @param ttlSeconds 過期時間（秒）
     * @return 加鎖成功返回 value，失敗返回 null
     */
    public String tryLock(String key, long ttlSeconds) {

        // 唯一標識，用於釋放鎖時校驗身份
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
     * 釋放分佈式鎖
     * @param key 鎖名
     * @param value 加鎖時返回的 UUID，用於身份校驗
     * @return 是否釋放成功
     */
    public boolean unlock(String key, String value) {

        // 當前鎖的 value，用來判斷鎖是否仍然屬於自己
        String current = redis.opsForValue().get(key);

        // 只有 value 匹配時才能釋放鎖
        if (value != null && value.equals(current)) {
            redis.delete(key);
            System.out.println("unlock ok: key=" + key);
            return true;
        }

        // 不匹配,鎖已過期或使用中，不能刪
        System.out.println("unlock skip: key=" + key);
        return false;
    }



}
