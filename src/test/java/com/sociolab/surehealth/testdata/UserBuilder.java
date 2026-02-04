package com.sociolab.surehealth.testdata;

import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.model.User;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test Data Builder for User entity.
 * - Provides sensible defaults
 * - Avoids hardcoded IDs
 * - Supports fluent overrides
 */
public final class UserBuilder {

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    private String name;
    private String email;
    private String password;
    private Role role;
    private AccountStatus status;
    private LocalDateTime createdAt;

    private UserBuilder() {
        int n = SEQ.getAndIncrement();
        this.name = "Test User " + n;
        this.email = "user" + n + "@example.com";
        this.password = "password"; // raw; encode before persisting
        this.role = Role.PATIENT;
        this.status = AccountStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public static UserBuilder aUser() {
        return new UserBuilder();
    }

    public UserBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public UserBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public UserBuilder withRole(Role role) {
        this.role = role;
        return this;
    }

    public UserBuilder withStatus(AccountStatus status) {
        this.status = status;
        return this;
    }

    public UserBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public User build() {
        User u = new User();
        // id intentionally left null (no hardcoded ids)
        u.setName(this.name);
        u.setEmail(this.email);
        u.setPassword(this.password);
        u.setRole(this.role);
        u.setStatus(this.status);
        u.setCreatedAt(this.createdAt);
        return u;
    }
}

