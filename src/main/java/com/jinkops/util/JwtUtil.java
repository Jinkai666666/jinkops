package com.jinkops.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

//JWT 工具类：生成与解析 Token

public class JwtUtil {

    // 建议生产环境放配置文件或环境变量
    private static final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // Token 有效期  1 小时
    private static final long EXPIRATION = 60 * 60 *1000;

    // 生成 Token
    public static String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key)
                .compact();
    }

    // 解析 Token
    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
