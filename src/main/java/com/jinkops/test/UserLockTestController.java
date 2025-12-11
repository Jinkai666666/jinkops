package com.jinkops.test;

import com.jinkops.entity.user.User;
import com.jinkops.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/lock/user")
@RequiredArgsConstructor
public class UserLockTestController {

    private final UserService userService;

    /**
     * 并发测试接口：
     * 多个请求同时创建同一个用户名
     */
    @PostMapping("/create")
    public String testCreateUser(@RequestBody User user) {

        try {
            userService.createUser(user);
            return "创建成功";
        } catch (Exception e) {
            log.error("create user error={}", e.getMessage());
            return "失败：" + e.getMessage();
        }
    }
}
