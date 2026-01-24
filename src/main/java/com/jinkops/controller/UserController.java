package com.jinkops.controller;

import com.jinkops.annotation.OperationLog;
import com.jinkops.annotation.RequirePermission;
import com.jinkops.entity.user.User;
import com.jinkops.service.UserService;
import com.jinkops.vo.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 查詢全部用戶
    @RequirePermission("sys:user:list")
    @GetMapping
    public ApiResponse<List<User>> getAllUsers() {
        log.info("[API] GET /api/users");
        return ApiResponse.success(userService.getAllUsers());
    }

    // 根據用戶名查詢（自動帶快取）
    @RequirePermission("sys:user:list")
    @GetMapping("/{username}")
    public ApiResponse<User> getUserByUsername(@PathVariable String username) {
        log.info("[API] GET /api/users/{username}");
        return ApiResponse.success(userService.findByUsername(username));
    }

    // 新增用戶
    @OperationLog("新增用戶")
    @RequirePermission("sys:user:update")
    @PostMapping
    public ApiResponse<User> addUser(@RequestBody User user) {
        log.info("[API] POST /api/users");
        return ApiResponse.success(userService.addUser(user));
    }

    @OperationLog("註冊用戶")
    @PostMapping("/register")
    public ApiResponse<User> register(@RequestBody User user) {
        log.info("[API] POST /api/users/register");
        return ApiResponse.success(userService.createUser(user));
    }



    // 刪除用戶
    @OperationLog("刪除用戶")
    @RequirePermission("sys:user:update")
    @DeleteMapping("/{username}")
    public ApiResponse<String> deleteUser(@PathVariable String username) {
        log.info("[API] DELETE /api/users/{username}");
        userService.deleteUser(username);
        return ApiResponse.success("User deleted: " + username, null);
    }

    // 更新用戶
    @OperationLog("更新用戶")
    @RequirePermission("sys:user:update")
    @PutMapping
    public ApiResponse<User> updateUser(@RequestBody User user) {
        log.info("[API] PUT /api/users");
        return ApiResponse.success(userService.updateUser(user));
    }

    // 分頁用戶查詢（帶快取）
    @RequirePermission("sys:user:list")
    @GetMapping("/page")
    public ApiResponse<Page<User>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("[API] GET /api/users/page");
        return ApiResponse.success(userService.pageUsers(page, size));
    }
}
