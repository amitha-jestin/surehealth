package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.LoginRequest;
import com.sociolab.surehealth.dto.LoginResponse;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.logging.LogUtil;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.UserRepository;
import com.sociolab.surehealth.security.SecurityUtil;
import com.sociolab.surehealth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenStore refreshTokenStore;
    private final LoginAttemptPolicy loginAttemptPolicy;
    private final LoginRateLimiter loginRateLimiter;
    private final RefreshTokenHasher refreshTokenHasher;

    // ================= LOGIN =================
    @Override
    public LoginResponse login(LoginRequest request) {

        log.info("Login attempt for email={}", LogUtil.maskEmail(request.email()));

        loginRateLimiter.checkAllowed(request.email());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found email={}", LogUtil.maskEmail(request.email()));
                    return new AppException(ErrorType.INVALID_CREDENTIALS, "Invalid email or password");
                });

        loginAttemptPolicy.validateLoginAllowed(user);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            loginAttemptPolicy.onFailedAttempt(user);
            log.warn("Login failed - invalid password email={}", LogUtil.maskEmail(user.getEmail()));
            throw new AppException(ErrorType.INVALID_CREDENTIALS, "Invalid email or password");
        }

        loginAttemptPolicy.onSuccessfulAttempt(user);

        // Generate tokens
        String accessToken = tokenService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = tokenService.generateRefreshToken(user.getId());

        // Store refresh token in Redis
        refreshTokenStore.saveRefreshToken(
                user.getId(),
                refreshTokenHasher.hash(refreshToken),
                tokenService.getRefreshExpirationSeconds()
        );

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
    @Override
    public void logout() {

        UserPrincipal principal = SecurityUtil.getCurrentUser();
        Long userId = principal.userId();

        String token = principal.accessToken();

        if (token != null) {


            // calculate remaining token expiration
            long expiry = tokenService.getRemainingExpirationSeconds(token);

            // blacklist access token in Redis
            refreshTokenStore.blacklistAccessToken(token, expiry);

            // delete refresh token
            refreshTokenStore.deleteRefreshToken(userId);

            log.info("User logout successful userId={}", userId);

        } else {
            log.warn("Logout attempted without valid token");
        }
    }

    // ================= REFRESH TOKEN =================
    @Override
    public Map<String, String> refreshAccessToken(String refreshToken) {

        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("Refresh token is missing");
            throw new AppException(ErrorType.INVALID_CREDENTIALS, "Refresh token is required");
        }
        try {
            tokenService.validateRefreshToken(refreshToken);
        } catch (Exception e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            throw new AppException(ErrorType.INVALID_CREDENTIALS, "Refresh token invalid or expired");
        }
        Long userId = tokenService.extractUserId(refreshToken);

        String storedToken = refreshTokenStore.getRefreshToken(userId);
        String hashedRefreshToken = refreshTokenHasher.hash(refreshToken);
        if (storedToken == null || !MessageDigest.isEqual(
                storedToken.getBytes(),
                hashedRefreshToken.getBytes()
        )) {
            log.warn("Invalid or expired refresh token userId={}", userId);
            // Possible token reuse or mismatch -> revoke all tokens for safety
            refreshTokenStore.revokeAllRefreshTokens(userId);
            throw new AppException(ErrorType.INVALID_CREDENTIALS, "Refresh token invalid or expired");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND, "User not found"));

        // Generate new tokens
        String newAccessToken = tokenService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String newRefreshToken = tokenService.generateRefreshToken(user.getId());

        // Save rotated refresh token in Redis
        refreshTokenStore.saveRefreshToken(
                user.getId(),
                refreshTokenHasher.hash(newRefreshToken),
                tokenService.getRefreshExpirationSeconds()
        );

        log.info("Refresh token successful userId={}", userId);

        return Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken
        );
    }

    // ================= PRIVATE METHODS =================
    // Policy and token handling delegated to dedicated components.
}
