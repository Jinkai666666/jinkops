package com.jinkops.controller;

import com.jinkops.annotation.OperationLog;
import com.jinkops.entity.user.User;
import com.jinkops.service.AuthService;
import com.jinkops.service.UserService;
import com.jinkops.vo.ApiResponse;
import com.jinkops.vo.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @OperationLog("註冊用戶")
    @PostMapping("/register")
    public ApiResponse<User> register(@RequestBody User user) {
        log.info("[API] POST /api/auth/register");
        return ApiResponse.success(userService.createUser(user));
    }
    // 登录接口
    @OperationLog
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody User user) {
        log.info("[API] POST /api/auth/login");
        return ApiResponse.success("登录成功", authService.login(user));
    }

    // 校验 Token
    @GetMapping("/verify")
    public ApiResponse<String> verify(@RequestParam String token) {
        log.info("[API] GET /api/auth/verify");
        return ApiResponse.success("Token 有效", authService.verify(token));
    }
}
