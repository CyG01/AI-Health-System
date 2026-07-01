package com.example.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.example.properties.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    /** 生产环境 JWT 密钥最小长度（字节） */
    private static final int MIN_SECRET_LENGTH_PROD = 32;

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;
    private final Environment environment;

    public JwtUtil(JwtProperties jwtProperties, Environment environment) {
        this.jwtProperties = jwtProperties;
        this.environment = environment;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 启动时校验 JWT 密钥强度。
     * 生产环境下密钥过短会直接抛出异常阻止启动，防止弱密钥被破解。
     */
    @PostConstruct
    public void checkSecretStrength() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT 密钥不能为空！请设置 jwt.secret 或 JWT_SECRET 环境变量");
        }

        // 检查是否为生产环境
        boolean isProd = false;
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                isProd = true;
                break;
            }
        }

        int secretLength = secret.getBytes(StandardCharsets.UTF_8).length;

        if (isProd) {
            // 生产环境强制校验
            if (secretLength < MIN_SECRET_LENGTH_PROD) {
                throw new IllegalStateException(
                        String.format("生产环境 JWT 密钥强度不足！当前长度: %d 字节，最小要求: %d 字节。"
                                        + "请设置足够强的 JWT_SECRET 环境变量（建议使用 openssl rand -base64 32 生成）",
                                secretLength, MIN_SECRET_LENGTH_PROD));
            }
            log.info("✅ JWT 密钥校验通过（生产环境），长度: {} 字节", secretLength);
        } else {
            // 开发环境仅警告
            if (secretLength < MIN_SECRET_LENGTH_PROD) {
                log.warn("⚠️  当前环境 JWT 密钥强度较弱（长度: {} 字节），生产环境请务必使用至少 {} 字节的强密钥",
                        secretLength, MIN_SECRET_LENGTH_PROD);
            } else {
                log.info("✅ JWT 密钥校验通过，长度: {} 字节", secretLength);
            }
        }
    }

    public String generateAccessToken(Long userId, String username, String role) {
        return buildToken(userId, username, role, jwtProperties.getAccessTokenExpire(), "access");
    }

    public String generateRefreshToken(Long userId) {
        return buildToken(userId, null, null, jwtProperties.getRefreshTokenExpire(), "refresh");
    }

    public String generateRefreshToken(Long userId, Long customExpireMs) {
        return buildToken(userId, null, null, customExpireMs, "refresh");
    }

    private String buildToken(Long userId, String username, String role, Long expireMillis, String tokenType) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireMillis);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .claim("tokenType", tokenType)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().after(new Date());
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 判断token是否已过期
     * 注意：即使token签名无效，只要能解析出过期时间，也会返回是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            // ExpiredJwtException说明token确实过期了
            return true;
        } catch (MalformedJwtException | SignatureException | IllegalArgumentException e) {
            // 其他解析错误，我们无法确定是否过期，返回false
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        Claims claims = parseToken(token);
        return "access".equals(claims.get("tokenType", String.class));
    }

    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    public String getRole(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    public Map<String, Object> buildTokenResponse(String accessToken, String refreshToken) {
        Map<String, Object> tokenMap = new HashMap<>(4);
        tokenMap.put("accessToken", accessToken);
        tokenMap.put("refreshToken", refreshToken);
        tokenMap.put("accessTokenExpire", jwtProperties.getAccessTokenExpire());
        tokenMap.put("refreshTokenExpire", jwtProperties.getRefreshTokenExpire());
        return tokenMap;
    }

    public String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String prefix = jwtProperties.getTokenPrefix();
        if (authorizationHeader.startsWith(prefix + " ")) {
            return authorizationHeader.substring(prefix.length() + 1);
        }
        return authorizationHeader;
    }
}
