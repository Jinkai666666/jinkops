package com.jinkops.controller;

import com.jinkops.annotation.OperationLog;
import com.jinkops.entity.user.User;
import com.jinkops.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
//  http://localhost:8080/api/users
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
        return userService.getByUsername(username);
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

}
