package com.jinkops.controller;

import com.jinkops.util.JwtUtil;
import com.jinkops.vo.ApiResponse;
import org.springframework.web.bind.annotation.*;

//登录接口测试

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 模拟登录：admin/123456 返回 Token
    @PostMapping("/login")
    public ApiResponse<String> login(@RequestParam String username,
                                     @RequestParam String password) {
        if ("admin".equals(username) && "123456".equals(password)) {
            String token = JwtUtil.generateToken(username);
            return ApiResponse.success("登录成功", token);
        }
        return ApiResponse.fail(401, "用户名或密码错误");
    }

    // 校验 Token
    @GetMapping("/verify")
    public ApiResponse<String> verify(@RequestParam String token) {
        try {
            String username = JwtUtil.parseToken(token).getSubject();
            return ApiResponse.success("Token有效：" + username, null);
        } catch (Exception e) {
            return ApiResponse.fail(401, "Token无效或已过期");
        }
    }
}
