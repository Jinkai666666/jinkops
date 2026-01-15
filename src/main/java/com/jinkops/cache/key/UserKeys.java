package com.jinkops.cache.key;


public class UserKeys {

    // 用戶資訊快取 key
    private static final String USER_INFO_PREFIX = "jinkops:user:info:";

    public static String userInfo(String username) {
        return USER_INFO_PREFIX + username;
    }
    // 分頁
    public static String userListPage(int page, int size) {
        return "user:list:page"+page+"-"+size;
    }
}
