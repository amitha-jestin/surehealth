package com.sociolab.surehealth.repository;

import com.sociolab.surehealth.model.MedicalCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicalCaseRepository extends JpaRepository<MedicalCase ,Long> {



    Optional<MedicalCase> findCaseById(Long caseId);

    Page<MedicalCase> findByDoctorId(Long doctorId, Pageable pageable);

    // Get cases for a patient with pagination
    Page<MedicalCase> findByPatientId(Long patientId, Pageable pageable);

}
