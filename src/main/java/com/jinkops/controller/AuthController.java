package com.jinkops.controller;

import com.jinkops.entity.user.User;
import com.jinkops.service.UserService;
import com.jinkops.util.JwtUtil;
import com.jinkops.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {


    private final AuthenticationManager authenticationManager;

    private final UserService userService;

    private final JwtUtil jwtUtil;
    //  登录接口
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            user.getPassword())
            );

            // 登录成功 生成 JWT
            String token = jwtUtil.generateToken(user.getUsername());
            result.put("code", 200);
            result.put("msg", "登录成功");
            result.put("token", token);
        } catch (AuthenticationException e) {
            result.put("code", 401);
            result.put("msg", "用户名或密码错误");
        }
        return result;
    }

    //  校验 Token
    @GetMapping("/verify")
    public ApiResponse<String> verify(@RequestParam String token) {
        try {
            String username = jwtUtil.parseToken(token).getSubject();
            return ApiResponse.success("Token有效：" + username, null);
        } catch (Exception e) {
            return ApiResponse.fail(401, "Token无效或已过期");
        }
    }




}
