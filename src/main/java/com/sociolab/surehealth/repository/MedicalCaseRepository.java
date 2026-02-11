package com.sociolab.surehealth.repository;

import com.sociolab.surehealth.model.MedicalCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalCaseRepository extends JpaRepository<MedicalCase ,Long> {

    List<MedicalCase> findByDoctorId(Long doctorId);

    List<MedicalCase> findByPatientId(Long id);
}
