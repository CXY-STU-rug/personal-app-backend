package com.liyuq.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secretStr;
    @Value("${jwt.expire-day}")
    private Integer expireDay;

    private SecretKey secretKey;

    // 初始化密钥：直接用配置字符串的 UTF-8 字节生成 HMAC 密钥。
    // 不做 Base64 解码——yml 里的 secret 是普通字符串（53字符），不是合法 Base64 串，
    // 硬解码会在启动时抛 IllegalArgumentException: Last unit does not have enough valid bits。
    // hmacShaKeyFor 要求密钥至少 32 字节（HS256），当前 53 字节满足
    @PostConstruct
    public void init() {
        secretKey = Keys.hmacShaKeyFor(secretStr.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成Token，存入userId
     */
    public String generateToken(Long userId) {
        long expireMs = expireDay * 24L * 60 * 60 * 1000;
        Date expireDate = new Date(System.currentTimeMillis() + expireMs);
        return Jwts.builder()
                .setSubject(userId.toString())
                .setExpiration(expireDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析Token，获取userId
     */
    public  Long getUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 校验token是否有效
     */
    public boolean validateToken(String token) {
        try {
            getUserId(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
