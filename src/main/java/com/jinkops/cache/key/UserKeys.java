package com.jinkops.cache.key;


public class UserKeys {

    // 用户信息缓存key
    public static String userInfo(String username) {
        return "user:info:" + username;
    }
}
