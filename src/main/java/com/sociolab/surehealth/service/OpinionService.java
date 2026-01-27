package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.OpinionRequest;
import com.sociolab.surehealth.dto.OpinionResponse;
import com.sociolab.surehealth.enums.CaseStatus;
import com.sociolab.surehealth.exception.ResourceNotFoundException;
import com.sociolab.surehealth.model.MedicalCase;
import com.sociolab.surehealth.model.Opinion;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.MedicalCaseRepository;
import com.sociolab.surehealth.repository.OpinionRepository;
import com.sociolab.surehealth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class OpinionService {

    private final OpinionRepository opinionRepository;
    private final MedicalCaseRepository caseRepository;
    private final UserRepository userRepository;

    @Transactional
    public OpinionResponse submitOpinion(Long caseId, String doctorEmail, OpinionRequest request) {
        // 1. Fetch case
        MedicalCase medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical case not found"));

        // 2. Ensure case is in ACCEPTED status
        if (medicalCase.getStatus() != CaseStatus.ACCEPTED) {
            throw new IllegalStateException(
                    "Doctor cannot submit opinion: Case is not accepted for review");
        }

        // 3. Fetch doctor
        User doctor = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        // 4. Optional: Verify doctor is assigned to the case (if workflow requires)
        if (!doctor.getId().equals(medicalCase.getDoctorId())) {
            throw new IllegalStateException(
                    "Doctor is not assigned to this case");
        }

        // 5. Create opinion entity
        Opinion opinion = new Opinion();
        opinion.setCaseId(medicalCase.getId());
        opinion.setDoctorId(doctor.getId());
        opinion.setComment(request.getComment());

        // 6. Save opinion
        Opinion savedOpinion = opinionRepository.save(opinion);

        // 7. Update case status to REVIEWED
        medicalCase.setStatus(CaseStatus.REVIEWED);
        caseRepository.save(medicalCase);

        // 8. Return response DTO
        return new OpinionResponse(
                savedOpinion.getId(),
                medicalCase.getId(),
                doctor.getId(),
                savedOpinion.getComment()
        );
    }
}
