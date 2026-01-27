package com.sociolab.surehealth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name ="opinions")
public class Opinion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private Long caseId;
    private Long doctorId;


    @Column(length = 5000)
    private String comment;

    private LocalDateTime createdAt = LocalDateTime.now();

}
