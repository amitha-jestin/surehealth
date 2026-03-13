package com.sociolab.surehealth.security;

import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.JwtAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class SecurityUtil {

    /**
     * Return an Optional containing the current UserPrincipal if present and authenticated.
     * Use this when you want to handle missing authentication gracefully.
     */
    public static Optional<UserPrincipal> getCurrentUserOptional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    /**
     * Existing behavior preserved: returns the current UserPrincipal or throws JwtAuthenticationException.
     * Implemented via getCurrentUserOptional() for consistent null-check handling.
     */
    public static UserPrincipal getCurrentUser() {
        return getCurrentUserOptional().orElseThrow(() ->
                new JwtAuthenticationException(
                        ErrorType.JWT_INVALID_TOKEN,
                        "User not authenticated or JWT missing/invalid"
                )
        );
    }

    /**
     * Optional variant for current user id.
     */
    public static Optional<Long> getCurrentUserIdOptional() {
        return getCurrentUserOptional().map(UserPrincipal::userId);
    }

    /**
     * Existing behavior preserved: returns current user id or throws.
     */
    public static Long getCurrentUserId() {
        return getCurrentUser().userId();
    }

    /**
     * Optional variant for current user email.
     */
    public static Optional<String> getCurrentUserEmailOptional() {
        return getCurrentUserOptional().map(UserPrincipal::email);
    }

    /**
     * Existing behavior preserved: returns current user email or throws.
     */
    public static String getCurrentUserEmail() {
        return getCurrentUser().email();
    }

    /**
     * Optional variant for current user role name.
     */
    public static Optional<String> getCurrentUserRoleOptional() {
        return getCurrentUserOptional().map(u -> u.role().name());
    }

    /**
     * Existing behavior preserved: returns current user role name or throws.
     */
    public static String getCurrentUserRole() {
        return getCurrentUser().role().name();
    }

    /**
     * Convenience helper: checks whether there is an authenticated principal in the SecurityContext.
     */
    public static boolean isAuthenticated() {
        return getCurrentUserOptional().isPresent();
    }
}
