package com.jinkops.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

//JWT 工具類：生成與解析 Token

@Component
public class JwtUtil {

    // 建議生產環境放配置文件或環境變量
    private  final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // Token 有效期  24 小時
    private  final long EXPIRATION_TIME = 24 * 60 * 60 * 1000;

    // 生成 Token
    public  String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_TIME);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SECRET_KEY)
                .compact();
    }

    // 解析 Token
    public  Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    // 檢查是否過期
    public  boolean isTokenExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    //JwtAuthenticationFilter 需要的方法：提取用戶名
    public String extractUsername(String token) {
        try {
            return parseToken(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    // JwtAuthenticationFilter 需要的方法：驗證 Token
    public boolean validateToken(String token, org.springframework.security.core.userdetails.UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username != null
                    && username.equals(userDetails.getUsername())
                    && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
