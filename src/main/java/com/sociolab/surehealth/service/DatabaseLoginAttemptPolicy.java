package com.sociolab.surehealth.service;

import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseLoginAttemptPolicy implements LoginAttemptPolicy {

    private final UserRepository userRepository;

    @Value("${security.login.max-attempts:5}")
    private int maxFailedAttempts;

    @Override
    public void validateLoginAllowed(User user) {
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

    @Override
    public void onFailedAttempt(User user) {
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

    @Override
    public void onSuccessfulAttempt(User user) {
        if (user.getFailedLoginAttempts() > 0 || user.getLockTime() != null) {
            log.info("Resetting failed login attempts userId={}", user.getId());
            user.setFailedLoginAttempts(0);
            user.setLockTime(null);
            userRepository.save(user);
        }
    }
}
