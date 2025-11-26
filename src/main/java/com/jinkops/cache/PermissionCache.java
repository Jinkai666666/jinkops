package com.jinkops.cache;

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
        return "perm:" + username;  // 正确的 key：perm:root
    }

    // 权限集合缓存到 Redis，缓存时长 24 小时
    // 例：set("root", ["sys:user:list", "sys:user:update"])
    public void set(String username, Set<String> perms) {
        redis.opsForValue().set(key(username), perms, 24, TimeUnit.HOURS);
    }


    // 从 Redis 中读取权限集合
    // 读不到就说明缓存不存在（可能过期）
    public Set<String> get(String username) {
        Object obj = redis.opsForValue().get(key(username));
        return obj == null ? null : (Set<String>) obj;
    }
    // 删除某个用户的权限缓存
    public void delete(String username) {
        redis.delete(key(username));
    }
}
