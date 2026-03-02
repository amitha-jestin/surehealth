package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.CaseNotificationEvent;
import com.sociolab.surehealth.dto.CaseRequest;
import com.sociolab.surehealth.dto.CaseResponse;
import com.sociolab.surehealth.enums.*;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.model.Doctor;
import com.sociolab.surehealth.model.MedicalCase;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.DoctorRepository;
import com.sociolab.surehealth.repository.MedicalCaseRepository;
import com.sociolab.surehealth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CaseService {

    private final MedicalCaseRepository caseRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final KafkaNotificationProducer kafkaProducer;

    // ================= SUBMIT CASE =================
    @Transactional
    public CaseResponse submitCase(String patientEmail, CaseRequest request) {
        User patient = userRepository.findByEmail(patientEmail)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND,
                        "Patient with email '" + patientEmail + "' not found"));

        Doctor doctor = doctorRepository.findByUserId(request.getDoctorId())
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND,
                        "Doctor with ID '" + request.getDoctorId() + "' not found"));

        validateDoctorStatus(doctor);

        MedicalCase medicalCase = new MedicalCase();
        medicalCase.setTitle(request.getTitle());
        medicalCase.setDescription(request.getDescription());
        medicalCase.setSpeciality(request.getSpeciality());
        medicalCase.setUrgency(request.getUrgency());
        medicalCase.setPatientId(patient.getId());
        medicalCase.setDoctorId(doctor.getUser().getId());
        medicalCase.setStatus(CaseStatus.ASSIGNED);
        medicalCase.setCreatedAt(LocalDateTime.now());

        MedicalCase savedCase = caseRepository.save(medicalCase);

        // Kafka notification
        kafkaProducer.sendEvent(new CaseNotificationEvent(
                doctor.getUser().getId(),
                "New case assigned: " + request.getTitle(),
                NotificationEventType.CASE_ASSIGNED
        ));

        return mapToResponse(savedCase);
    }

    // ================= ACCEPT CASE =================
    @Transactional
    public CaseResponse acceptCase(Long caseId, String doctorEmail) {
        MedicalCase medicalCase = getCaseForDoctorAction(caseId, doctorEmail);
        medicalCase.setStatus(CaseStatus.ACCEPTED);

        kafkaProducer.sendEvent(new CaseNotificationEvent(
                medicalCase.getPatientId(),
                "Doctor accepted your case",
                NotificationEventType.CASE_ACCEPTED
        ));

        return mapToResponse(medicalCase);
    }

    // ================= REJECT CASE =================
    @Transactional
    public CaseResponse rejectCase(Long caseId, String doctorEmail) {
        MedicalCase medicalCase = getCaseForDoctorAction(caseId, doctorEmail);
        medicalCase.setStatus(CaseStatus.REJECTED);

        kafkaProducer.sendEvent(new CaseNotificationEvent(
                medicalCase.getPatientId(),
                "Doctor rejected your case",
                NotificationEventType.CASE_REJECTED
        ));

        return mapToResponse(medicalCase);
    }

    // ================= GET MY CASES (SPRING PAGE) =================
    public Page<CaseResponse> getMyCases(String email, int page, int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND,
                        "User with email '" + email + "' not found"));

        if (user.getRole() != Role.DOCTOR && user.getRole() != Role.PATIENT) {
            throw new AppException(ErrorType.ACCESS_DENIED,
                    "Only doctors and patients can access their cases");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<MedicalCase> casesPage = switch (user.getRole()) {
            case DOCTOR -> caseRepository.findByDoctorId(user.getId(), pageable);
            case PATIENT -> caseRepository.findByPatientId(user.getId(), pageable);
            default -> throw new AppException(ErrorType.ACCESS_DENIED,
                    "Role not allowed to access cases");
        };

        // Map MedicalCase -> CaseResponse
        return casesPage.map(this::mapToResponse);
    }

    // ================= HELPER METHODS =================
    private MedicalCase getCaseForDoctorAction(Long caseId, String doctorEmail) {
        MedicalCase medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND,
                        "Case with ID '" + caseId + "' not found"));

        User user = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND,
                        "User with email '" + doctorEmail + "' not found"));

        Doctor doctor = doctorRepository.findById(user.getId())
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND,
                        "Doctor not found"));

        validateDoctorStatus(doctor);

        if (!medicalCase.getDoctorId().equals(doctor.getUser().getId())) {
            throw new AppException(ErrorType.ACCESS_DENIED,
                    "You are not assigned to this case");
        }

        if (medicalCase.getStatus() != CaseStatus.ASSIGNED) {
            throw new AppException(ErrorType.INVALID_OPERATION,
                    "Case already processed or not available for action");
        }

        return medicalCase;
    }

    private void validateDoctorStatus(Doctor doctor) {
        if (doctor.getUser().getStatus() != AccountStatus.ACTIVE) {
            throw new AppException(ErrorType.USER_INVALID_STATUS,
                    "Doctor account is not active");
        }
    }

    private CaseResponse mapToResponse(MedicalCase c) {
        return new CaseResponse(
                c.getId(),
                c.getTitle(),
                c.getDescription(),
                c.getSpeciality(),
                c.getUrgency(),
                c.getStatus(),
                c.getCreatedAt(),
                c.getDoctorId()
        );
    }
}