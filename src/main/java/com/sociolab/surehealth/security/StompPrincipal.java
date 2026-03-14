package com.sociolab.surehealth.security;

import java.security.Principal;

/**
 * Lightweight Principal used for STOMP/WebSocket authentication.
 * We keep it simple: name() returns the user's email (used by Spring for simpUser).
 */
public class StompPrincipal implements Principal {
    private final Long userId;
    private final String email;
    private final String role;
    private final String token;

    public StompPrincipal(Long userId, String email, String role, String token) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.token = token;
    }

    @Override
    public String getName() {
        return email;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getToken() {
        return token;
    }
}

