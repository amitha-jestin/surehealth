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
    public CaseResponse submitCase(Long userId, CaseRequest request) {
        log.debug("action=case_submit status=NOOP layer=service method=submitCase userId={} doctorId={}", userId, request.getDoctorId());

        User patient = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("action=case_submit status=FAILED userId={} reason=PATIENT_NOT_FOUND", userId);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND);
                });

        Doctor doctor = doctorRepository.findByUserId(request.getDoctorId())
                .orElseThrow(() -> {
                    log.warn("action=case_submit status=FAILED doctorId={} reason=DOCTOR_NOT_FOUND",
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

        log.info("action=case_submit status=SUCCESS caseId={} userId={} doctorId={}",
                savedCase.getId(), patient.getId(), doctor.getUser().getId());

        // enqueue outbox event for reliable async delivery
        outboxService.enqueue(
                "CASE_CREATED",
                "MedicalCase",
                savedCase.getId().toString(),
                new CaseNotificationEvent(
                        java.util.UUID.randomUUID().toString(),
                        doctor.getUser().getId(),
                        "New case assigned: " + request.getTitle(),
                        null,
                        CaseStatus.ASSIGNED,
                        "Patient ID: " + patient.getId(),
                        LocalDateTime.now()
                )
        );

        log.debug("action=case_submit status=SUCCESS caseId={}", savedCase.getId());

        return mapToResponse(savedCase);
    }

    // ================= UPDATE CASE STATUS =================

    @Override
    @Transactional
    public CaseResponse updateCaseStatus(Long caseId, Long doctorId, CaseStatus status) {
        log.debug("action=case_status_update status=NOOP layer=service method=updateCaseStatus caseId={} userId={} newStatus={}", caseId, doctorId, status);

        MedicalCase medicalCase = getCaseForDoctorAction(caseId, doctorId);
        CaseStatus oldStatus = medicalCase.getStatus();
        medicalCase.setStatus(status);

        log.info("action=case_status_update status=SUCCESS caseId={} newStatus={}",
                caseId, status);


        outboxService.enqueue("CASE_STATUS_UPDATED",
                "MedicalCase",
                medicalCase.getId().toString(),
                new CaseNotificationEvent(
                        java.util.UUID.randomUUID().toString(),
                        medicalCase.getPatient() != null ? medicalCase.getPatient().getId() : null,
                        "Doctor changed your case status to " + status,
                        oldStatus,
                        status,
                        "Doctor ID: " + doctorId,
                        LocalDateTime.now()

                )
        );

        log.debug("action=case_status_update status=SUCCESS caseId={}", caseId);


        return mapToResponse(medicalCase);
    }

    // ================= GET MY CASES =================
    @Override
    public Page<CaseResponse> getMyCases(Long userId, int page, int size) {
        log.debug("action=case_my_fetch status=NOOP layer=service method=getMyCases userId={} page={} size={}", userId, page, size);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("action=case_my_fetch status=FAILED userId={} reason=USER_NOT_FOUND", userId);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND);
                });

        if (user.getRole() != Role.DOCTOR && user.getRole() != Role.PATIENT) {
            log.warn("action=case_my_fetch status=FAILED userId={} reason=ACCESS_DENIED role={}",
                    user.getId(), user.getRole());
            throw new AppException(ErrorType.ACCESS_DENIED);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<MedicalCase> casesPage = switch (user.getRole()) {
            case DOCTOR -> caseRepository.findByDoctor_Id(user.getId(), pageable);
            case PATIENT -> caseRepository.findByPatient_Id(user.getId(), pageable);
            default -> throw new AppException(ErrorType.ACCESS_DENIED);
        };

        log.info("action=case_my_fetch status=SUCCESS userId={} count={}",
                user.getId(), casesPage.getTotalElements());

        return casesPage.map(this::mapToResponse);
    }

    // ================= HELPER METHODS =================
    private MedicalCase getCaseForDoctorAction(Long caseId, Long doctorId) {

        MedicalCase medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> {
                    log.warn("action=case_status_update status=FAILED caseId={} reason=CASE_NOT_FOUND", caseId);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND);
                });

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> {
                    log.warn("action=case_status_update status=FAILED userId={} reason=DOCTOR_NOT_FOUND", doctorId);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND);
                });

        validateDoctorStatus(doctor);

        if (!medicalCase.getDoctor().getId().equals(doctor.getUser().getId())) {
            log.warn("action=case_status_update status=FAILED caseId={} userId={} reason=NOT_ASSIGNED",
                    caseId, doctor.getUser().getId());
            throw new AppException(ErrorType.ACCESS_DENIED);
        }

        if (medicalCase.getStatus() != CaseStatus.ASSIGNED) {
            log.warn("action=case_status_update status=FAILED caseId={} reason=INVALID_STATUS currentStatus={}",
                    caseId, medicalCase.getStatus());
            throw new AppException(ErrorType.INVALID_OPERATION);
        }

        return medicalCase;
    }

    private void validateDoctorStatus(Doctor doctor) {
        if (doctor.getUser().getStatus() != AccountStatus.ACTIVE) {
            log.warn("action=case_status_update status=FAILED userId={} reason=DOCTOR_INACTIVE",
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
