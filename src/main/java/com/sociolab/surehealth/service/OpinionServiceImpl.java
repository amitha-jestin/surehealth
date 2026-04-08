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
import com.sociolab.surehealth.logging.LogUtil;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpinionServiceImpl implements OpinionService {

    private final OpinionRepository opinionRepository;
    private final MedicalCaseRepository caseRepository;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public OpinionResponse submitOpinion(Long caseId, Long doctorId, OpinionRequest request) {
        log.debug("action=opinion_submit status=NOOP layer=service method=submitOpinion caseId={} userId={}", caseId, doctorId);

        // 1. Fetch case
        MedicalCase medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> {
                    log.warn("action=opinion_submit status=FAILED caseId={} reason=CASE_NOT_FOUND", caseId);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND, "Medical case not found");
                });

        // 2. Ensure case is in ACCEPTED status
        if (medicalCase.getStatus() != CaseStatus.ACCEPTED) {
            log.warn("action=opinion_submit status=FAILED caseId={} reason=INVALID_STATUS currentStatus={}", caseId, medicalCase.getStatus());
            throw new AppException(ErrorType.INVALID_OPERATION,
                    "Opinions can only be submitted for cases in ACCEPTED status");
        }

        // 3. Fetch doctor
        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> {
                    log.warn("action=opinion_submit status=FAILED userId={} reason=DOCTOR_NOT_FOUND", doctorId);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND, "Doctor not found");
                });

        // 4. Verify doctor is assigned to the case
        if (!doctor.getId().equals(medicalCase.getDoctor().getId())) {
            log.warn("action=opinion_submit status=FAILED caseId={} userId={} reason=NOT_ASSIGNED", caseId, doctor.getId());
            throw new AppException(ErrorType.INVALID_OPERATION, "You are not assigned to review this case");
        }

        // 5. Create and save opinion
        Opinion opinion = new Opinion();
        opinion.setMedicalCase(medicalCase);
        opinion.setDoctor(doctor);
        opinion.setComment(request.getComment());

        Opinion savedOpinion = opinionRepository.save(opinion);
        log.info("action=opinion_submit status=SUCCESS opinionId={}", savedOpinion.getId());

        // 6. Update case status to REVIEWED
        medicalCase.setStatus(CaseStatus.REVIEWED);
        caseRepository.save(medicalCase);
        log.info("action=opinion_submit status=SUCCESS caseId={} newStatus=REVIEWED", caseId);

        // 7. Return response DTO
        return new OpinionResponse(
                savedOpinion.getId(),
                doctor.getId(),
                medicalCase.getId(),
                savedOpinion.getComment()
        );
    }
}
