package com.sociolab.surehealth.model;

import com.sociolab.surehealth.enums.Role;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role; // PATIENT / DOCTOR / ADMIN

    private LocalDateTime createdAt = LocalDateTime.now();

}
