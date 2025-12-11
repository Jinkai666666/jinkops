package com.jinkops.controller;

import com.jinkops.annotation.OperationLog;
import com.jinkops.entity.user.User;
import com.jinkops.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

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
    @OperationLog("查詢全部用戶")
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // 根據用戶名查詢（自動帶緩存）
    @OperationLog("根據用戶名查詢用戶")
    @GetMapping("/{username}")
    public User getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username);
    }

    // 新增用戶
    @OperationLog("新增用戶")
    @PostMapping
    public User addUser(@RequestBody User user) {
        return userService.addUser(user);
    }

    // 刪除用戶
    @OperationLog("刪除用戶")
    @DeleteMapping("/{username}")
    public String deleteUser(@PathVariable String username) {
        userService.deleteUser(username);
        return "User deleted: " + username;
    }

    // 更新用戶
    @OperationLog("更新用戶")
    @PutMapping
    public User updateUser(@RequestBody User user) {
        return userService.updateUser(user);
    }

    // 分頁用戶查詢（帶緩存）
    @GetMapping("/page")
    public Page<User> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return userService.pageUsers(page, size);
    }
}
