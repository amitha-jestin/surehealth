package com.sociolab.surehealth.repository;

import com.sociolab.surehealth.model.MedicalCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicalCaseRepository extends JpaRepository<MedicalCase ,Long> {



    Optional<MedicalCase> findCaseById(Long caseId);

    // Find by nested property (doctor.user id)
    Page<MedicalCase> findByDoctor_Id(Long doctorId, Pageable pageable);

    // Get cases for a patient with pagination using nested property
    Page<MedicalCase> findByPatient_Id(Long patientId, Pageable pageable);

}
