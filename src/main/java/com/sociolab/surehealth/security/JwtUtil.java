package com.sociolab.surehealth.security;

import com.sociolab.surehealth.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration.access}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 characters"
            );
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }


    /* =====================================================
       TOKEN CREATION
       ===================================================== */

    public String generateToken(Long id, String email, Role role) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("role", role.name())
                .claim("id", id)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /* =====================================================
       TOKEN PARSING (throws JwtException)
       ===================================================== */

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(String refreshToken) {
        Claims claims = extractAllClaims(refreshToken);
        String userIdStr = claims.getSubject();
        return Long.parseLong(userIdStr);
    }

    public Long extractUserIdFromAccessToken(String accessToken) {
            Claims claims = extractAllClaims(accessToken);
            return claims.get("id", Long.class);
        }

    public long getRefreshExpirationSeconds() {
        return refreshExpirationMs / 1000;
    }

    public long getRemainingExpiration(String token) {
        Claims claims = extractAllClaims(token);
        Date expiration = claims.getExpiration();
        long now = System.currentTimeMillis();
        long expiryTime = expiration.getTime();
        return Math.max(0, (expiryTime - now) / 1000);
    }
}
