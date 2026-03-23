package com.sociolab.surehealth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private final RedisService redisService;

    @Override
    public void saveRefreshToken(Long userId, String refreshToken, long expirationSeconds) {
        redisService.saveRefreshToken(userId, refreshToken, expirationSeconds);
    }

    @Override
    public String getRefreshToken(Long userId) {
        return redisService.getRefreshToken(userId);
    }

    @Override
    public void deleteRefreshToken(Long userId) {
        redisService.deleteRefreshToken(userId);
    }

    @Override
    public void blacklistAccessToken(String token, long expirationSeconds) {
        redisService.blacklistToken(token, expirationSeconds);
    }

    @Override
    public void revokeAllRefreshTokens(Long userId) {
        redisService.revokeRefreshTokens(userId);
    }
}
