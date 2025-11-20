package com.jinkops.controller;

import com.jinkops.annotation.RequirePermission;
import com.jinkops.enums.PermissionMode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestPermissionController {

    // 普通权限（所有带 sys:user:list 的用户都能访问）
    @RequirePermission("sys:user:list")
    @GetMapping("/api/test/list")
    public String list() {
        return "OK: sys:user:list";
    }

    //只有管理员有的权限
    @RequirePermission("sys:user:update")
    @GetMapping("/api/test/update")
    public String update() {
        return "OK: sys:user:update";
    }

    //  AND 必须两个权限都有
    @RequirePermission(
            value = {"sys:config:read", "sys:config:reset"},
            mode = PermissionMode.AND
    )
    @GetMapping("/api/test/config/reset")
    public String configReset() {
        return "OK: sys:config:read + sys:config:reset (AND)";
    }
}
