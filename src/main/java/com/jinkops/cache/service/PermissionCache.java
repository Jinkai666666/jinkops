package com.jinkops.cache.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class PermissionCache {

    private final RedisTemplate<String, Object> redis;

    public PermissionCache(RedisTemplate<String, Object> redis) {
        this.redis = redis;
    }

    // Redis 的 key，例 perm:admin、perm:root
    private String key(String username) {
        return "perm:" + username; // 正確的 key：perm:root
    }

    // 權限集合快取到 Redis，快取時長 24 小時
    // 例：set("root", ["sys:user:list", "sys:user:update"])
    public void set(String username, Set<String> perms) {
        redis.opsForValue().set(key(username), perms, 24, TimeUnit.HOURS);
    }

    // 從 Redis 中讀取權限集合
    // 讀不到就表示快取不存在（可能過期）
    public Set<String> get(String username) {
        Object obj = redis.opsForValue().get(key(username));
        return obj == null ? null : (Set<String>) obj;
    }

    // 刪除某個用戶的權限快取
    public void delete(String username) {
        redis.delete(key(username));
    }
}
