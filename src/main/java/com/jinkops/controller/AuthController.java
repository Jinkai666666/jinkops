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
    //  登錄接口
    @OperationLog("用戶登錄")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody User user) {

        try {
            // Security 認證賬號密碼
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            user.getPassword())
            );

            // 從userdetails獲取權限集合
            org.springframework.security.core.userdetails.User userDetails =
                    (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

            //權限
            var authorities = userDetails.getAuthorities();
            //權限轉成能用的字符串 Set
            Set<String> perms = authorities.stream()
                    .map(a -> a.getAuthority())
                    .collect(Collectors.toSet());

            // 寫入 Redis
            permissionCache.set(user.getUsername(), perms);

            // 生成 token
            String token = jwtUtil.generateToken(user.getUsername());

            //構建 LoginResponse
            LoginResponse resp = new LoginResponse();
            resp.setToken(token);

            return ApiResponse.success("登錄成功", resp);

        } catch (AuthenticationException e) {
            return ApiResponse.fail(401, "用戶名或密碼錯誤");
        }
    }


    //  校驗 Token
    @GetMapping("/verify")
    public ApiResponse<String> verify(@RequestParam String token) {
        try {
            String username = jwtUtil.parseToken(token).getSubject();
            return ApiResponse.success("Token有效：" ,username);
        } catch (Exception e) {
            return ApiResponse.fail(401, "Token無效或已過期");
        }
    }




}
