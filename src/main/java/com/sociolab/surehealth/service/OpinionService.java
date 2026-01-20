package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.OpinionRequest;
import com.sociolab.surehealth.enums.CaseStatus;
import com.sociolab.surehealth.model.MedicalCase;
import com.sociolab.surehealth.model.Opinion;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.MedicalCaseRepository;
import com.sociolab.surehealth.repository.OpinionRepository;
import com.sociolab.surehealth.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class OpinionService {
    private final OpinionRepository opinionRepository;
    private final MedicalCaseRepository caseRepository;
    private final UserRepository userRepository;

    public OpinionService(OpinionRepository opinionRepository,
                          MedicalCaseRepository caseRepository,
                          UserRepository userRepository) {
        this.opinionRepository = opinionRepository;
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
    }

    public Opinion submitOpinion(Long caseId, Long doctorId, OpinionRequest opinionreq) {
        MedicalCase medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found"));

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        Opinion opinion = new Opinion();
        opinion.setMedicalCase(medicalCase);
        opinion.setDoctor(doctor);
        opinion.setComment(opinionreq.getComment());

        medicalCase.setStatus(CaseStatus.REVIEWED);
        caseRepository.save(medicalCase);

        return opinionRepository.save(opinion);
    }


}
