package com.jinkops.controller;

import com.jinkops.entity.User;
import com.jinkops.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users") // 基础路径
public class UserController {

    private final UserService userService;


    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 查全部用户
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // 按用户名查
    @GetMapping("/{username}")
    public User getUserByUsername(@PathVariable String username) {
        return userService.getByUsername(username);
    }
}
