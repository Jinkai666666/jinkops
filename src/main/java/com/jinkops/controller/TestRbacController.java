package com.jinkops.controller;

import com.jinkops.annotation.RequirePermission;
import com.jinkops.enums.PermissionMode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestRbacController {

    // 無需權限
    @GetMapping("/api/test/open")
    public String open() {
        return "公開接口：無需任何權限";
    }

    // 普通權限：sys:user:list
    @RequirePermission("sys:user:list")
    @GetMapping("/api/test/user/list")
    public String userList() {
        return "正常訪問：你擁有 sys:user:list 權限";
    }

    // 管理員權限：sys:user:update
    @RequirePermission("sys:user:update")
    @GetMapping("/api/test/user/update")
    public String userUpdate() {
        return "正常訪問：你擁有 sys:user:update 權限（管理員權限）";
    }

    // 多權限 AND：sys:config:read + sys:config:reset 都要有
    @RequirePermission(
            value = {"sys:config:read", "sys:config:reset"},
            mode = PermissionMode.AND
    )
    @GetMapping("/api/test/config/reset")
    public String configReset() {
        return "正常訪問：sys:config:read + sys:config:reset 兩個權限（AND）";
    }
}
