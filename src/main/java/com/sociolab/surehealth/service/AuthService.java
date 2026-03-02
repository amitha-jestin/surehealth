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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${security.login.max-attempts:5}")
    private int maxFailedAttempts;

    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AppException(ErrorType.INVALID_CREDENTIALS));

        validateAccountStatus(user);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            handleFailedAttempt(user);
            throw new AppException(ErrorType.INVALID_CREDENTIALS);
        }

        resetLoginAttempts(user);

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

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
        }
    }

    // ================= PRIVATE METHODS =================

    private void validateAccountStatus(User user) {
        if (user.getStatus() == AccountStatus.BLOCKED) {
            throw new AppException(ErrorType.USER_BLOCKED);
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new AppException(ErrorType.USER_PENDING);
        }
    }

    private void handleFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= maxFailedAttempts) {
            user.setStatus(AccountStatus.BLOCKED);
            user.setLockTime(LocalDateTime.now());
        }

        userRepository.save(user);
    }

    private void resetLoginAttempts(User user) {
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
