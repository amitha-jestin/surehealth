package com.sociolab.surehealth.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name ="opinions")
public class Opinion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private MedicalCase medicalCase;

    @ManyToOne
    private User doctor;

    @Column(length = 5000)
    private String comment;

    private LocalDateTime createdAt = LocalDateTime.now();

}
