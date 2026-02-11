package com.sociolab.surehealth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    @JsonIgnore
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role; // PATIENT / DOCTOR / ADMIN

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    private LocalDateTime createdAt = LocalDateTime.now();

    private int failedLoginAttempts = 0;
    private LocalDateTime lockTime;

}
