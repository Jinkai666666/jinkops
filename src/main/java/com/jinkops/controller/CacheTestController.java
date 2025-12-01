package com.jinkops.controller;

import com.jinkops.cache.key.UserKeys;
import com.jinkops.cache.service.CacheService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cache")
public class CacheTestController {

    private final CacheService cache;

    public CacheTestController(CacheService cache) {
        this.cache = cache;
    }

    @GetMapping("/set")
    public String set(@RequestParam String username) {
        cache.set(UserKeys.userInfo(username), "hello-" + username, 3600);
        return "set ok";
    }

    @GetMapping("/get")
    public String get(@RequestParam String username) {
        return cache.get(UserKeys.userInfo(username));
    }

    @GetMapping("/del")
    public String del(@RequestParam String username) {
        cache.delete(UserKeys.userInfo(username));
        return "del ok";
    }
}
