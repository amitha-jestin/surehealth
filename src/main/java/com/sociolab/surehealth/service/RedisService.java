package com.sociolab.surehealth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    // ---------------- Refresh Token ----------------

    public void saveRefreshToken(Long userId, String refreshToken, long expiration) {
        String key = "refresh_token:" + userId;
        redisTemplate.opsForValue().set(key, refreshToken, expiration, TimeUnit.SECONDS);
    }

    public String getRefreshToken(Long userId) {
        return redisTemplate.opsForValue().get("refresh_token:" + userId);
    }

    public void deleteRefreshToken(Long userId) {
        redisTemplate.delete("refresh_token:" + userId);
    }

    public void revokeRefreshTokens(Long userId) {
        redisTemplate.delete("refresh_token:" + userId);
        redisTemplate.delete("refresh_token_prev:" + userId);
    }

    public void savePreviousRefreshToken(Long userId, String refreshToken, long expiration) {
        String key = "refresh_token_prev:" + userId;
        redisTemplate.opsForValue().set(key, refreshToken, expiration, TimeUnit.SECONDS);
    }

    public String getPreviousRefreshToken(Long userId) {
        return redisTemplate.opsForValue().get("refresh_token_prev:" + userId);
    }

    // ---------------- Token Blacklist ----------------

    public void blacklistToken(String token, long expiration) {
        String key = "blacklist_token:" + token;
        redisTemplate.opsForValue().set(key, "blacklisted", expiration, TimeUnit.SECONDS);
    }

    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey("blacklist_token:" + token)
        );
    }

    public long incrementWithExpiry(String key, long ttlSeconds) {
        Long value = redisTemplate.opsForValue().increment(key);
        if (value != null && value == 1L) {
            redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        }
        return value != null ? value : 0L;
    }

    public boolean setIfAbsent(String key, String value, long ttlSeconds) {
        Boolean set = redisTemplate.opsForValue().setIfAbsent(key, value, ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(set);
    }

    public void clearAll() {
        // For testing purposes only - clears all keys in Redis
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }
}
