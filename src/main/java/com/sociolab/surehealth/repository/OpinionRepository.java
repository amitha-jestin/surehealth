package com.sociolab.surehealth.repository;

import java.util.List;

import com.sociolab.surehealth.model.Opinion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpinionRepository extends JpaRepository<Opinion , Long> {
    // Query by nested property (doctor's id)
    List<Opinion> findByDoctor_Id(Long doctorId);

}
