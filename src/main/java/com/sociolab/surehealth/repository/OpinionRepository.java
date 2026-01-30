package com.sociolab.surehealth.repository;

import java.util.List;

import com.sociolab.surehealth.model.Opinion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpinionRepository extends JpaRepository<Opinion , Long> {
    List<Opinion> findByDoctorId(Long doctorId);

}
