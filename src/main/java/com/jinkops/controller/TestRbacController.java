package com.jinkops.controller;

import com.jinkops.annotation.RequirePermission;
import com.jinkops.enums.PermissionMode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestRbacController {

    // 无需权限
    @GetMapping("/api/test/open")
    public String open() {
        return "公开接口：无需任何权限";
    }

    // 普通权限：sys:user:list
    @RequirePermission("sys:user:list")
    @GetMapping("/api/test/user/list")
    public String userList() {
        return "正常访问：你拥有 sys:user:list 权限";
    }

    // 管理员权限：sys:user:update
    @RequirePermission("sys:user:update")
    @GetMapping("/api/test/user/update")
    public String userUpdate() {
        return "正常访问：你拥有 sys:user:update 权限（管理员权限）";
    }

    // 多权限 AND：sys:config:read + sys:config:reset 都要有
    @RequirePermission(
            value = {"sys:config:read", "sys:config:reset"},
            mode = PermissionMode.AND
    )
    @GetMapping("/api/test/config/reset")
    public String configReset() {
        return "正常访问：sys:config:read + sys:config:reset 两个权限（AND）";
    }
}
