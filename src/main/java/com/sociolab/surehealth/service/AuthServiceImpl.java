package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.LoginRequest;
import com.sociolab.surehealth.dto.LoginResponse;
import com.sociolab.surehealth.dto.RefreshTokenResponse;
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
        log.debug("action=auth_login status=NOOP layer=service method=login email={}", LogUtil.maskEmail(request.email()));

        loginRateLimiter.checkAllowed(request.email());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("action=auth_login status=FAILED reason=USER_NOT_FOUND email={}", LogUtil.maskEmail(request.email()));
                    return new AppException(ErrorType.INVALID_CREDENTIALS, "Invalid email or password");
                });

        loginAttemptPolicy.validateLoginAllowed(user);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            loginAttemptPolicy.onFailedAttempt(user);
            log.warn("action=auth_login status=FAILED reason=INVALID_PASSWORD email={}", LogUtil.maskEmail(user.getEmail()));
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

        log.info("action=auth_login status=SUCCESS userId={} role={}", user.getId(), user.getRole());

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
    public void logout(Long userId, String token) {
        log.debug("action=auth_logout status=NOOP layer=service method=logout userId={}", userId);

        if (token != null) {

            // calculate remaining token expiration
            long expiry = tokenService.getRemainingExpirationSeconds(token);

            // blacklist access token in Redis
            refreshTokenStore.blacklistAccessToken(token, expiry);

            // delete refresh token
            refreshTokenStore.deleteRefreshToken(userId);

            log.info("action=auth_logout status=SUCCESS userId={}", userId);

        } else {
            log.info("action=auth_logout status=NOOP reason=NO_TOKEN");
        }
    }

    // ================= REFRESH TOKEN =================
    @Override
    public RefreshTokenResponse refreshAccessToken(String refreshToken) {
        log.debug("action=auth_refresh status=NOOP layer=service method=refreshAccessToken");

        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("action=auth_refresh status=FAILED reason=MISSING_REFRESH_TOKEN");
            throw new AppException(ErrorType.INVALID_CREDENTIALS, "Refresh token is required");
        }
        try {
            tokenService.validateRefreshToken(refreshToken);
        } catch (Exception e) {
            log.warn("action=auth_refresh status=FAILED reason=INVALID_REFRESH_TOKEN message={}", e.getMessage());
            throw new AppException(ErrorType.INVALID_CREDENTIALS, "Refresh token invalid or expired");
        }
        Long userId = tokenService.extractUserId(refreshToken);

        String storedToken = refreshTokenStore.getRefreshToken(userId);
        String hashedRefreshToken = refreshTokenHasher.hash(refreshToken);
        if (storedToken == null || !MessageDigest.isEqual(
                storedToken.getBytes(),
                hashedRefreshToken.getBytes()
        )) {
            log.warn("action=auth_refresh status=FAILED userId={} reason=TOKEN_MISMATCH", userId);
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

        log.info("action=auth_refresh status=SUCCESS userId={}", userId);

        return new RefreshTokenResponse(
                newAccessToken,
                newRefreshToken
        );
    }

    // ================= PRIVATE METHODS =================
    // Policy and token handling delegated to dedicated components.
}
