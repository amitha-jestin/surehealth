package com.sociolab.surehealth.service;

public interface RefreshTokenStore {

    void saveRefreshToken(Long userId, String refreshToken, long expirationSeconds);

    String getRefreshToken(Long userId);

    void deleteRefreshToken(Long userId);

    void blacklistAccessToken(String token, long expirationSeconds);

    void revokeAllRefreshTokens(Long userId);
}
