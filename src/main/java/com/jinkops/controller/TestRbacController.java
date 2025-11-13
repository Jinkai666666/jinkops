package com.jinkops.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestRbacController {

    // 所有人都可以访问
    @GetMapping("/api/test/open")
    public String open() {
        return "接口正常";
    }

    // 需要 sys:user:list 权限
    @PreAuthorize("hasAuthority('sys:user:list')")
    @GetMapping("/api/test/list")
    public String userList() {
        return " sys:user:list 权限（普通用户 + 管理员都能访问）";
    }

    // 需要 sys:user:update 权限
    @PreAuthorize("hasAuthority('sys:user:update')")
    @GetMapping("/api/test/update")
    public String userUpdate() {
        return "sys:user:update 权限（只有管理员能访问）";
    }
}
