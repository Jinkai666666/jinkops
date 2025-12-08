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

    // 查询全部用户
    @OperationLog("查询全部用户")
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // 根据用户名查询（自动带缓存）
    @OperationLog("根据用户名查询用户")
    @GetMapping("/{username}")
    public User getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username);
    }

    // 新增用户
    @OperationLog("新增用户")
    @PostMapping
    public User addUser(@RequestBody User user) {
        return userService.addUser(user);
    }

    // 删除用户
    @OperationLog("删除用户")
    @DeleteMapping("/{username}")
    public String deleteUser(@PathVariable String username) {
        userService.deleteUser(username);
        return "User deleted: " + username;
    }

    // 更新用户
    @OperationLog("更新用户")
    @PutMapping
    public User updateUser(@RequestBody User user) {
        return userService.updateUser(user);
    }

    // 分页用户查询（带缓存）
    @GetMapping("/page")
    public Page<User> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return userService.pageUsers(page, size);
    }
}
