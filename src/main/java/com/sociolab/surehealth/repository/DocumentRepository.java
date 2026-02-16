package com.sociolab.surehealth.repository;

import com.sociolab.surehealth.model.MedicalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<MedicalDocument, Long> {

    List<MedicalDocument> findByMedicalCaseId(Long caseId);
}
