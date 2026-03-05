package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.OpinionRequest;
import com.sociolab.surehealth.dto.OpinionResponse;
import com.sociolab.surehealth.enums.CaseStatus;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.model.MedicalCase;
import com.sociolab.surehealth.model.Opinion;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.MedicalCaseRepository;
import com.sociolab.surehealth.repository.OpinionRepository;
import com.sociolab.surehealth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpinionService {

    private final OpinionRepository opinionRepository;
    private final MedicalCaseRepository caseRepository;
    private final UserRepository userRepository;

    @Transactional
    public OpinionResponse submitOpinion(Long caseId, String doctorEmail, OpinionRequest request) {
        log.info("Submitting opinion for caseId={} by doctorEmail={}", caseId, doctorEmail);

        // 1. Fetch case
        MedicalCase medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> {
                    log.warn("Medical case not found caseId={}", caseId);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND, "Medical case not found");
                });

        // 2. Ensure case is in ACCEPTED status
        if (medicalCase.getStatus() != CaseStatus.ACCEPTED) {
            log.warn("Case not in ACCEPTED status caseId={} currentStatus={}", caseId, medicalCase.getStatus());
            throw new AppException(ErrorType.INVALID_OPERATION,
                    "Opinions can only be submitted for cases in ACCEPTED status");
        }

        // 3. Fetch doctor
        User doctor = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> {
                    log.warn("Doctor not found email={}", doctorEmail);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND, "Doctor not found");
                });

        // 4. Verify doctor is assigned to the case
        if (!doctor.getId().equals(medicalCase.getDoctorId())) {
            log.warn("Doctor not assigned to case caseId={} doctorId={}", caseId, doctor.getId());
            throw new AppException(ErrorType.INVALID_OPERATION, "You are not assigned to review this case");
        }

        // 5. Create and save opinion
        Opinion opinion = new Opinion();
        opinion.setCaseId(medicalCase.getId());
        opinion.setDoctorId(doctor.getId());
        opinion.setComment(request.getComment());

        Opinion savedOpinion = opinionRepository.save(opinion);
        log.debug("Opinion saved opinionId={}", savedOpinion.getId());

        // 6. Update case status to REVIEWED
        medicalCase.setStatus(CaseStatus.REVIEWED);
        caseRepository.save(medicalCase);
        log.info("Case status updated to REVIEWED caseId={}", caseId);

        // 7. Return response DTO
        return new OpinionResponse(
                savedOpinion.getId(),
                medicalCase.getId(),
                doctor.getId(),
                savedOpinion.getComment()
        );
    }
}