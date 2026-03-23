package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.CaseNotificationEvent;
import com.sociolab.surehealth.dto.CaseRequest;
import com.sociolab.surehealth.dto.CaseResponse;
import com.sociolab.surehealth.enums.*;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.logging.LogUtil;
import com.sociolab.surehealth.model.Doctor;
import com.sociolab.surehealth.model.MedicalCase;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.DoctorRepository;
import com.sociolab.surehealth.repository.MedicalCaseRepository;
import com.sociolab.surehealth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseServiceImpl implements CaseService {

    private final MedicalCaseRepository caseRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final OutboxService outboxService;

    // ================= SUBMIT CASE =================
    @Transactional
    @Override
    public CaseResponse submitCase(String patientEmail, CaseRequest request) {

        String maskedPatient = LogUtil.maskEmail(patientEmail);
        log.info("Submitting case title='{}' patientEmail={} doctorId={}",
                request.getTitle(), maskedPatient, request.getDoctorId());

        User patient = userRepository.findByEmail(patientEmail)
                .orElseThrow(() -> {
                    log.warn("Case submission failed - patient not found email={}", maskedPatient);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND);
                });

        Doctor doctor = doctorRepository.findByUserId(request.getDoctorId())
                .orElseThrow(() -> {
                    log.warn("Case submission failed - doctor not found doctorId={} ",
                            request.getDoctorId());
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND);
                });

        validateDoctorStatus(doctor);

        MedicalCase medicalCase = new MedicalCase();
        medicalCase.setTitle(request.getTitle());
        medicalCase.setDescription(request.getDescription());
        medicalCase.setSpeciality(request.getSpeciality());
        medicalCase.setUrgency(request.getUrgency());
        medicalCase.setPatient(patient);
        medicalCase.setDoctor(doctor.getUser());
        medicalCase.setStatus(CaseStatus.ASSIGNED);
        medicalCase.setCreatedAt(LocalDateTime.now());

        MedicalCase savedCase = caseRepository.save(medicalCase);

        log.info("Case created successfully caseId={} patientId={} doctorId={}",
                savedCase.getId(), patient.getId(), doctor.getUser().getId());

        // enqueue outbox event for reliable async delivery
        outboxService.enqueue(
                "CASE_ASSIGNED",
                "MedicalCase",
                savedCase.getId().toString(),
                new CaseNotificationEvent(
                        java.util.UUID.randomUUID().toString(),
                        doctor.getUser().getId(),
                        "New case assigned: " + request.getTitle(),
                        NotificationEventType.CASE_ASSIGNED
                )
        );

        log.debug("Outbox event enqueued for case assignment caseId={}", savedCase.getId());

        return mapToResponse(savedCase);
    }

    // ================= ACCEPT CASE =================
    @Transactional
    @Override
    public CaseResponse acceptCase(Long caseId, String doctorEmail) {

        String maskedDoctor = LogUtil.maskEmail(doctorEmail);
        log.info("Doctor attempting to accept case caseId={} doctorEmail={}",
                caseId, maskedDoctor);

        MedicalCase medicalCase = getCaseForDoctorAction(caseId, doctorEmail);
        medicalCase.setStatus(CaseStatus.ACCEPTED);

        log.info("Case accepted caseId={} doctorId={}",
                caseId, medicalCase.getDoctor().getId());

        outboxService.enqueue(
                "CASE_ACCEPTED",
                "MedicalCase",
                medicalCase.getId().toString(),
                new CaseNotificationEvent(
                        java.util.UUID.randomUUID().toString(),
                        medicalCase.getPatient() != null ? medicalCase.getPatient().getId() : null,
                        "Doctor accepted your case",
                        NotificationEventType.CASE_ACCEPTED
                )
        );

        log.debug("Outbox event enqueued for case acceptance caseId={}", caseId);

        return mapToResponse(medicalCase);
    }

    // ================= REJECT CASE =================
    @Transactional
    @Override
    public CaseResponse rejectCase(Long caseId, String doctorEmail) {

        String maskedDoctor = LogUtil.maskEmail(doctorEmail);
        log.info("Doctor attempting to reject case caseId={} doctorEmail={}",
                caseId, maskedDoctor);

        MedicalCase medicalCase = getCaseForDoctorAction(caseId, doctorEmail);
        medicalCase.setStatus(CaseStatus.REJECTED);

        log.info("Case rejected caseId={} doctorId={}",
                caseId, medicalCase.getDoctor().getId());

        outboxService.enqueue(
                "CASE_REJECTED",
                "MedicalCase",
                medicalCase.getId().toString(),
                new CaseNotificationEvent(
                        java.util.UUID.randomUUID().toString(),
                        medicalCase.getPatient() != null ? medicalCase.getPatient().getId() : null,
                        "Doctor rejected your case",
                        NotificationEventType.CASE_REJECTED
                )
        );

        log.debug("Outbox event enqueued for case rejection caseId={}", caseId);

        return mapToResponse(medicalCase);
    }

    // ================= GET MY CASES =================
    @Override
    public Page<CaseResponse> getMyCases(String email, int page, int size) {

        String masked = LogUtil.maskEmail(email);
        log.debug("Fetching cases email={} page={} size={}", masked, page, size);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Get cases failed - user not found email={}", masked);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND);
                });

        if (user.getRole() != Role.DOCTOR && user.getRole() != Role.PATIENT) {
            log.warn("Unauthorized case access attempt userId={} role={}",
                    user.getId(), user.getRole());
            throw new AppException(ErrorType.ACCESS_DENIED);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<MedicalCase> casesPage = switch (user.getRole()) {
            case DOCTOR -> caseRepository.findByDoctor_Id(user.getId(), pageable);
            case PATIENT -> caseRepository.findByPatient_Id(user.getId(), pageable);
            default -> throw new AppException(ErrorType.ACCESS_DENIED);
        };

        log.debug("Cases fetched count={} userId={}",
                casesPage.getTotalElements(), user.getId());

        return casesPage.map(this::mapToResponse);
    }

    // ================= HELPER METHODS =================
    private MedicalCase getCaseForDoctorAction(Long caseId, String doctorEmail) {

        MedicalCase medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> {
                    log.warn("Case not found caseId={}", caseId);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND);
                });

        String maskedDoctor = LogUtil.maskEmail(doctorEmail);
        User user = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> {
                    log.warn("Doctor user not found email={}", maskedDoctor);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND);
                });

        Doctor doctor = doctorRepository.findById(user.getId())
                .orElseThrow(() -> {
                    log.warn("Doctor entity not found userId={}", user.getId());
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND);
                });

        validateDoctorStatus(doctor);

        if (!medicalCase.getDoctor().getId().equals(doctor.getUser().getId())) {
            log.warn("Unauthorized case action attempt caseId={} doctorId={}",
                    caseId, doctor.getUser().getId());
            throw new AppException(ErrorType.ACCESS_DENIED);
        }

        if (medicalCase.getStatus() != CaseStatus.ASSIGNED) {
            log.warn("Invalid case action caseId={} status={}",
                    caseId, medicalCase.getStatus());
            throw new AppException(ErrorType.INVALID_OPERATION);
        }

        return medicalCase;
    }

    private void validateDoctorStatus(Doctor doctor) {
        if (doctor.getUser().getStatus() != AccountStatus.ACTIVE) {
            log.warn("Inactive doctor attempted action doctorId={}",
                    doctor.getUser().getId());
            throw new AppException(ErrorType.USER_INVALID_STATUS);
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
                c.getDoctor() != null ? c.getDoctor().getId() : null
        );
    }
}
