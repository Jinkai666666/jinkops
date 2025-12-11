package com.jinkops.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
/**
 * 分布式锁
 * 所有业务：加锁/解锁
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class LockService{
    private final StringRedisTemplate redis;

    //加锁：返回 value（身份标识）
    public String tryLock(String key , long ttlSeconds) {
        String value = UUID.randomUUID().toString();
        boolean ok = redis.opsForValue().setIfAbsent(
                key, value, ttlSeconds, TimeUnit.SECONDS);
        if (ok) {
            log.info("[LOCK OK] key={} value={}", key, value);
            return value;
        }
        log.info("[LOCK FAIL] key={}", key);
        return null;
    }

    //解鎖 ，只能刪除自己的鎖
    public void unlock(String key ,String value){
        try{
            String current = redis.opsForValue().get(key);
            if(value.equals(current)){
                redis.delete(key);
                log.info("[UNLOCK OK] key={} value={}", key, value);
            }else {
                log.warn("[UNLOCK FAIL] key={} 當前鎖非本人持有", key);
            }
        }catch (Exception e){
            log.error("[UNLOCK ERROR] key={} error={}", key, e.getMessage(), e);
        }
    }


}

