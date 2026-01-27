package com.sociolab.surehealth.repository;

import com.sociolab.surehealth.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface DoctorRepository extends JpaRepository<Doctor, Long> {
  //  boolean existsByEmail(String email);

    boolean existsByLicenseNumber(String licenseNumber);

   Optional<Doctor> findByUserId(Long userId);

   Optional<Doctor> findByEmail(String Email);

}
