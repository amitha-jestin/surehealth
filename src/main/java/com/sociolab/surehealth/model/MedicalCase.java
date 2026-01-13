package com.sociolab.surehealth.model;

import com.sociolab.surehealth.enums.CaseStatus;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@NoArgsConstructor
@Entity
@Table(name="cases")
public class MedicalCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @ManyToOne
    private User patient;

    private String filePath; // where report/photo is stored

    @Enumerated(EnumType.STRING)
    private CaseStatus status; // SUBMITTED / REVIEWED

    private LocalDateTime createdAt = LocalDateTime.now();

}
