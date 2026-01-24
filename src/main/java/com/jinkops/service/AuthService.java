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

    // 登入流程：成功則回傳 token
    public LoginResponse login(User user) {
        long start = System.currentTimeMillis();
        log.info("[SERVICE] login start keyParams=username={}", user.getUsername());
        try {
            // 交給 Spring Security 驗證帳密
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            user.getPassword())
            );

            // 從 userDetails 取出權限集合
            org.springframework.security.core.userdetails.User userDetails =
                    (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

            // 權限轉成可用的字串 Set
            Set<String> perms = userDetails.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .filter(s -> s != null)
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());

            // 寫入 Redis 快取
            permissionCache.set(user.getUsername(), perms);

            // 生成 token
            String token = jwtUtil.generateToken(user.getUsername());

            // 組裝 LoginResponse
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

    // 只做 JWT 解碼與校驗，不需要登入態
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
