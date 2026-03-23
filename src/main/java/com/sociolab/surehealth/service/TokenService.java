package com.sociolab.surehealth.service;

import com.sociolab.surehealth.enums.Role;
import io.jsonwebtoken.Claims;

public interface TokenService {

    String generateAccessToken(Long id, String email, Role role);

    String generateRefreshToken(Long userId);

    void validateRefreshToken(String refreshToken);

    Long extractUserId(String refreshToken);

    long getRefreshExpirationSeconds();

    long getRemainingExpirationSeconds(String token);

    Claims extractAllClaims(String token);
}
