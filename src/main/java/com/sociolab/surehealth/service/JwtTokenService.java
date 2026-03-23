package com.sociolab.surehealth.service;

import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenService implements TokenService {

    private final JwtUtil jwtUtil;

    @Override
    public String generateAccessToken(Long id, String email, Role role) {
        return jwtUtil.generateToken(id, email, role);
    }

    @Override
    public String generateRefreshToken(Long userId) {
        return jwtUtil.generateRefreshToken(userId);
    }

    @Override
    public void validateRefreshToken(String refreshToken) {
        jwtUtil.validateRefreshToken(refreshToken);
    }

    @Override
    public Long extractUserId(String refreshToken) {
        return jwtUtil.extractUserId(refreshToken);
    }

    @Override
    public long getRefreshExpirationSeconds() {
        return jwtUtil.getRefreshExpirationSeconds();
    }

    @Override
    public long getRemainingExpirationSeconds(String token) {
        return jwtUtil.getRemainingExpiration(token);
    }

    @Override
    public Claims extractAllClaims(String token) {
        return jwtUtil.extractAllClaims(token);
    }
}
