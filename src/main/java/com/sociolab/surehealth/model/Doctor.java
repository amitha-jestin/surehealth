package com.sociolab.surehealth.model;

import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.enums.Speciality;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
        name = "doctors",
        uniqueConstraints = {@UniqueConstraint(columnNames = "licenseNumber")
        }
)

public class Doctor {

    @Id
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    private Speciality speciality;

    private int experienceYears;

    private boolean verified;




}
