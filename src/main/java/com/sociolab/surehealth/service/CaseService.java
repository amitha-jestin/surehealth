package com.sociolab.surehealth.service;

import com.sociolab.surehealth.enums.CaseStatus;
import com.sociolab.surehealth.model.MedicalCase;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.MedicalCaseRepository;
import com.sociolab.surehealth.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class CaseService {

    private final MedicalCaseRepository caseRepository;
    private final UserRepository userRepository;

    public CaseService(MedicalCaseRepository caseRepository, UserRepository userRepository) {
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
    }

    public MedicalCase submitCase(Long patientId, MedicalCase medicalCase) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        medicalCase.setPatient(patient);
        medicalCase.setStatus(CaseStatus.SUBMITTED);
        return caseRepository.save(medicalCase);
    }

}
