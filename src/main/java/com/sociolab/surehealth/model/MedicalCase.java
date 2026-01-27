package com.sociolab.surehealth.model;

import com.sociolab.surehealth.enums.CaseStatus;
import com.sociolab.surehealth.enums.Speciality;
import com.sociolab.surehealth.enums.Urgency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name="cases")
public class MedicalCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;
    private Long doctorId;
    private String title;
    private String description;
    @Enumerated(EnumType.STRING)
    private Speciality speciality;
    @Enumerated(EnumType.STRING)
    private Urgency urgency;

    private String filePath; // where report/photo is stored

    @Enumerated(EnumType.STRING)
    private CaseStatus status; // SUBMITTED / REVIEWED

    private LocalDateTime createdAt = LocalDateTime.now();

}
