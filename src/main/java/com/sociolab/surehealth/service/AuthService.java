package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.LoginRequest;
import com.sociolab.surehealth.dto.LoginResponse;
import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.AppException;
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


import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${security.login.max-attempts:5}")
    private int maxFailedAttempts;

    public LoginResponse login(LoginRequest request) {

        log.info("Login attempt for email={}", request.email());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found email={}", request.email());
                    return new AppException(ErrorType.INVALID_CREDENTIALS);
                });

        validateAccountStatus(user);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            handleFailedAttempt(user);
            log.warn("Login failed - invalid password email={}", user.getEmail());
            throw new AppException(ErrorType.INVALID_CREDENTIALS);
        }

        resetLoginAttempts(user);

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        log.info("Login successful userId={} role={}", user.getId(), user.getRole());

        return new LoginResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    public void logout(HttpServletRequest request) {
        String token = extractToken(request);

        if (token != null) {
            tokenBlacklistService.blacklistToken(token);
            log.info("User logout successful");
        } else {
            log.warn("Logout attempted without valid token");
        }
    }

    // ================= PRIVATE METHODS =================

    private void validateAccountStatus(User user) {

        if (user.getStatus() == AccountStatus.BLOCKED) {
            log.warn("Blocked user attempted login userId={}", user.getId());
            throw new AppException(ErrorType.USER_BLOCKED);
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            log.warn("Inactive user attempted login userId={} status={}",
                    user.getId(), user.getStatus());
            throw new AppException(ErrorType.USER_PENDING);
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

        userRepository.save(user);
    }

    private void resetLoginAttempts(User user) {

        if (user.getFailedLoginAttempts() > 0) {
            log.info("Resetting failed login attempts userId={}", user.getId());
        }

        user.setFailedLoginAttempts(0);
        user.setLockTime(null);
        userRepository.save(user);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        return (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : null;
    }
}
