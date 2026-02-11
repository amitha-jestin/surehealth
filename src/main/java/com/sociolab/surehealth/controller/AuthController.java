package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.LoginRequest;
import com.sociolab.surehealth.dto.LoginResponse;
import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.exception.InvalidCredentialsException;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.UserRepository;
import com.sociolab.surehealth.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private static final int MAX_FAILED_ATTEMPTS = 5;


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if(user.getStatus() == AccountStatus.BLOCKED) {
                throw new InvalidCredentialsException("Account is blocked . Please contact support.");
            }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {

            // Increment failed attempts
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            // Lock account if max attempts reached
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setStatus(AccountStatus.BLOCKED);
                user.setLockTime(java.time.LocalDateTime.now());
            }

            userRepository.save(user);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidCredentialsException("Account not active yet. Please wait for admin approval.");
        }

        user.setFailedLoginAttempts(0);
        user.setLockTime(null);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(),user.getEmail(), user.getRole());
        return ResponseEntity.ok(new LoginResponse(token, user.getId(),user.getEmail(), user.getRole().name()));
    }

    
}
