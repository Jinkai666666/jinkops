package com.jinkops.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 分散式鎖
 * 所有業務：加鎖/解鎖
 */
@Service
@RequiredArgsConstructor
public class LockService {
    private final StringRedisTemplate redis;

    // 加鎖：返回 value（身分識別）
    public String tryLock(String key, long ttlSeconds) {
        String value = UUID.randomUUID().toString();
        boolean ok = redis.opsForValue().setIfAbsent(
                key, value, ttlSeconds, TimeUnit.SECONDS);
        if (ok) {
            return value;
        }
        return null;
    }

    // 解鎖，只能刪除自己的鎖
    public void unlock(String key, String value) {
        try {
            String current = redis.opsForValue().get(key);
            if (value.equals(current)) {
                redis.delete(key);
            } else {
            }
        } catch (Exception e) {
        }
    }
}
