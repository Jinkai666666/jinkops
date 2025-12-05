package com.jinkops.controller;

import com.jinkops.annotation.OperationLog;
import com.jinkops.entity.user.User;
import com.jinkops.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
//  http://localhost:8080/api/users
@Slf4j
@RestController
@RequestMapping("/api/users") // 基础路径
public class UserController {

    private final UserService userService;


    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 查全部用户
    @OperationLog("查询全部用户")
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // 按用户名查
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

    //删除用户
    @OperationLog("删除用户")
    @DeleteMapping("/{username}")
    public String deleteUser(@PathVariable String username) {
        userService.deleteUser(username);
        return "User deleted: " + username;
    }

    @GetMapping("/info")
    public User getUserInfo(@RequestParam String username) {

        // 查缓存
        User cached = userService.getUserFromCache(username);
        if (cached != null) {
            log.info("cache hit user: {}", username);
            return cached;
        }

        log.info("cache miss user: {}", username);

        // 查数据库
        User user = userService.findByUsername(username);
        if (user != null) {
            // 写缓存
            userService.setUserCache(username, user);
        }

        return user;
    }
    //更新用户
    @OperationLog("更新用户")
    @PutMapping
    public User updateUser(@RequestBody User user) {
        return userService.updateUser(user);
    }


    @GetMapping("/page")
    public Page<User> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return userService.pageUsers(page, size);
    }



}
