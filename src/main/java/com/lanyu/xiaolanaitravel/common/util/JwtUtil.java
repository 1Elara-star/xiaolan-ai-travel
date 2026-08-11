package com.lanyu.xiaolanaitravel.common.util;

import com.lanyu.xiaolanaitravel.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** 负责生成、解析和验证 JWT。 */
@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 登录成功后生成包含用户 ID、用户名和角色的令牌。 */
    public String generateToken(User user) {
        Date now = new Date();
        Date expirationTime = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(expirationTime)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    public Long getUserId(String token) {
        Number userId = parseToken(token).get("userId", Number.class);
        return userId.longValue();
    }

    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            return parseToken(token).getExpiration().after(new Date());
        } catch (Exception exception) {
            return false;
        }
    }
}
