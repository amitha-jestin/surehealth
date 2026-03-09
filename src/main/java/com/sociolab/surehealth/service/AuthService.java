package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.LoginRequest;
import com.sociolab.surehealth.dto.LoginResponse;
import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.logging.LogUtil;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.UserRepository;
import com.sociolab.surehealth.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisService redisService;

    @Value("${security.login.max-attempts:5}")
    private int maxFailedAttempts;

    // ================= LOGIN =================
    public LoginResponse login(LoginRequest request) {

        log.info("Login attempt for email={}", LogUtil.maskEmail(request.email()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found email={}", LogUtil.maskEmail(request.email()));
                    return new AppException(ErrorType.INVALID_CREDENTIALS, "Invalid email or password");
                });

        validateAccountStatus(user);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            handleFailedAttempt(user);
            log.warn("Login failed - invalid password email={}", LogUtil.maskEmail(user.getEmail()));
            throw new AppException(ErrorType.INVALID_CREDENTIALS, "Invalid email or password");
        }

        resetLoginAttempts(user);

        // Generate tokens
        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Store refresh token in Redis
        redisService.saveRefreshToken(user.getId(), refreshToken, jwtUtil.getRefreshExpirationSeconds());

        log.info("Login successful userId={} role={}", user.getId(), user.getRole());

        return new LoginResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    // ================= LOGOUT =================
    public void logout(HttpServletRequest request) {

        String token = extractToken(request);

        if (token != null) {

            Long userId = jwtUtil.extractUserIdFromAccessToken(token);

            // calculate remaining token expiration
            long expiry = jwtUtil.getRemainingExpiration(token);

            // blacklist access token in Redis
            redisService.blacklistToken(token, expiry);

            // delete refresh token
            redisService.deleteRefreshToken(userId);

            log.info("User logout successful userId={}", userId);

        } else {
            log.warn("Logout attempted without valid token");
        }
    }

    // ================= REFRESH TOKEN =================
    public Map<String, String> refreshAccessToken(String refreshToken) {

        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("Refresh token is missing");
            throw new AppException(ErrorType.INVALID_CREDENTIALS, "Refresh token is required");
        }

        Long userId = jwtUtil.extractUserId(refreshToken);

        String storedToken = redisService.getRefreshToken(userId);
        if (storedToken == null || !MessageDigest.isEqual(
                storedToken.getBytes(),
                refreshToken.getBytes()
        )) {
            log.warn("Invalid or expired refresh token userId={}", userId);
            throw new AppException(ErrorType.INVALID_CREDENTIALS, "Refresh token invalid or expired");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND, "User not found"));

        // Generate new tokens
        String newAccessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Save rotated refresh token in Redis
        redisService.saveRefreshToken(user.getId(), newRefreshToken, jwtUtil.getRefreshExpirationSeconds());

        log.info("Refresh token successful userId={}", userId);

        return Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken
        );
    }

    // ================= PRIVATE METHODS =================
    private void validateAccountStatus(User user) {
        if (user.getStatus() == AccountStatus.BLOCKED) {
            log.warn("Blocked user attempted login userId={}", user.getId());
            throw new AppException(ErrorType.USER_BLOCKED, "User is blocked");
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            log.warn("Inactive user attempted login userId={} status={}",
                    user.getId(), user.getStatus());
            throw new AppException(ErrorType.USER_PENDING, "User account is not active");
        }
    }

    private void handleFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        log.warn("Failed login attempt {} for userId={}", attempts, user.getId());

        if (attempts >= maxFailedAttempts) {
            user.setStatus(AccountStatus.BLOCKED);
            user.setLockTime(LocalDateTime.now());
            log.error("User auto-blocked due to max failed attempts userId={}", user.getId());
        }

        userRepository.save(user); // Save after updating attempts/status
    }

    private void resetLoginAttempts(User user) {
        if (user.getFailedLoginAttempts() > 0 || user.getLockTime() != null) {
            log.info("Resetting failed login attempts userId={}", user.getId());
            user.setFailedLoginAttempts(0);
            user.setLockTime(null);
            userRepository.save(user);
        }
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        return (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7).trim()
                : null;
    }
}