package com.jinkops.service;

import com.jinkops.cache.service.PermissionCache;
import com.jinkops.entity.user.User;
import com.jinkops.exception.BizException;
import com.jinkops.exception.ErrorCode;
import com.jinkops.util.JwtUtil;
import com.jinkops.vo.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PermissionCache permissionCache;
    private final JwtUtil jwtUtil;

    // 登入流程，成功就回 token
    public LoginResponse login(User user) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] login start keyParams=username={}", user.getUsername());
        try {
            // Security 認證帳號密碼
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            user.getPassword())
            );

            // 從 userdetails 獲取權限集合
            org.springframework.security.core.userdetails.User userDetails =
                    (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

            // 權限轉成能用的字串 Set
            Set<String> perms = userDetails.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .collect(Collectors.toSet());

            // 寫入 Redis
            permissionCache.set(user.getUsername(), perms);

            // 生成 token
            String token = jwtUtil.generateToken(user.getUsername());

            // 構建 LoginResponse
            LoginResponse resp = new LoginResponse();
            resp.setToken(token);
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] login success cost={}ms keyResult=user={}", cost, user.getUsername());
            return resp;

        } catch (AuthenticationException e) {
            log.error("[SERVICE] login failed reason={}", e.getMessage(), e);
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }

    // 只做驗證與解析，失敗就拋未登入
    public String verify(String token) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] verify start keyParams=token");
        try {
            String username = jwtUtil.parseToken(token).getSubject();
            long cost = System.currentTimeMillis() - start;
            log.info("[SERVICE] verify success cost={}ms keyResult=user={}", cost, username);
            return username;
        } catch (Exception e) {
            log.error("[SERVICE] verify failed reason={}", e.getMessage(), e);
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }
}
