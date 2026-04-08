package com.sociolab.surehealth.service;

import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.logging.LogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoginRateLimiter {

    private final RedisService redisService;

    @Value("${security.login.rate-limit.max-attempts:10}")
    private int maxAttempts;

    @Value("${security.login.rate-limit.window-seconds:300}")
    private long windowSeconds;

    public void checkAllowed(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        String key = "login:rate:" + email.toLowerCase();
        long attempts = redisService.incrementWithExpiry(key, windowSeconds);
        if (attempts > maxAttempts) {
            log.warn("action=auth_login_rate_limit status=FAILED email={} attempts={}", LogUtil.maskEmail(email), attempts);
            throw new AppException(ErrorType.TOO_MANY_REQUESTS,
                    "Too many login attempts. Please try again later.");
        }
    }
}
