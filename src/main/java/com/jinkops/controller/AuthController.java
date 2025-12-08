package com.jinkops.controller;

import com.jinkops.annotation.OperationLog;
import com.jinkops.cache.service.PermissionCache;
import com.jinkops.entity.user.User;
import com.jinkops.service.UserService;
import com.jinkops.util.JwtUtil;
import com.jinkops.vo.ApiResponse;
import com.jinkops.vo.LoginResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {


    private final AuthenticationManager authenticationManager;

    private final UserService userService;

    private final PermissionCache permissionCache;

    private final JwtUtil jwtUtil;
    //  登录接口
    @OperationLog("用户登录")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody User user) {

        try {
            // Security 认证账号密码
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            user.getPassword())
            );

            // 从userdetails获取权限集合
            org.springframework.security.core.userdetails.User userDetails =
                    (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

            //权限
            var authorities = userDetails.getAuthorities();
            //权限转成能用的字符串 Set
            Set<String> perms = authorities.stream()
                    .map(a -> a.getAuthority())
                    .collect(Collectors.toSet());

            // 写入 Redis
            permissionCache.set(user.getUsername(), perms);

            // 生成 token
            String token = jwtUtil.generateToken(user.getUsername());

            //构建 LoginResponse
            LoginResponse resp = new LoginResponse();
            resp.setToken(token);

            return ApiResponse.success("登录成功", resp);

        } catch (AuthenticationException e) {
            return ApiResponse.fail(401, "用户名或密码错误");
        }
    }


    //  校验 Token
    @GetMapping("/verify")
    public ApiResponse<String> verify(@RequestParam String token) {
        try {
            String username = jwtUtil.parseToken(token).getSubject();
            return ApiResponse.success("Token有效：" ,username);
        } catch (Exception e) {
            return ApiResponse.fail(401, "Token无效或已过期");
        }
    }




}
