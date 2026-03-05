package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.DoctorResponse;
import com.sociolab.surehealth.dto.PatientSummary;
import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.model.Doctor;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.DoctorRepository;
import com.sociolab.surehealth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private static final String USER_CREATED_AT = "createdAt";
    private static final String DOCTOR_USER_CREATED_AT = "user.createdAt";

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    // ================== APPROVE DOCTOR ==================

    @Transactional
    public void approveDoctor(Long doctorId) {

        log.info("APPROVE_DOCTOR_ATTEMPT: doctorId={} traceId={}",
                doctorId, MDC.get("traceId"));

        Doctor doctor = doctorRepository.findByUserId(doctorId)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND));

        AccountStatus status = doctor.getUser().getStatus();

        if (status == AccountStatus.ACTIVE) {
            log.warn("APPROVE_DOCTOR_FAILED: doctorId={} reason=ALREADY_ACTIVE traceId={}",
                    doctorId, MDC.get("traceId"));
            throw new AppException(ErrorType.USER_ACTIVE);
        }

        if (status == AccountStatus.BLOCKED) {
            log.warn("APPROVE_DOCTOR_FAILED: doctorId={} reason=BLOCKED traceId={}",
                    doctorId, MDC.get("traceId"));
            throw new AppException(ErrorType.USER_BLOCKED);
        }

        doctor.getUser().setRole(Role.DOCTOR);
        doctor.getUser().setStatus(AccountStatus.ACTIVE);

        log.info("APPROVE_DOCTOR_SUCCESS: doctorId={} traceId={}",
                doctorId, MDC.get("traceId"));
    }

    // ================== BLOCK USER ==================

    @Transactional
    public void blockUser(Long userId) {

        log.info("BLOCK_USER_ATTEMPT: userId={} traceId={}",
                userId, MDC.get("traceId"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND));

        if (user.getStatus() == AccountStatus.BLOCKED) {
            log.warn("BLOCK_USER_FAILED: userId={} reason=ALREADY_BLOCKED traceId={}",
                    userId, MDC.get("traceId"));
            throw new AppException(ErrorType.USER_BLOCKED);
        }

        user.setStatus(AccountStatus.BLOCKED);

        log.info("BLOCK_USER_SUCCESS: userId={} traceId={}",
                userId, MDC.get("traceId"));
    }

    // ================== UNBLOCK USER ==================

    @Transactional
    public void unblockUser(Long userId) {

        log.info("UNBLOCK_USER_ATTEMPT: userId={} traceId={}",
                userId, MDC.get("traceId"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND));

        if (user.getStatus() == AccountStatus.ACTIVE) {
            log.warn("UNBLOCK_USER_FAILED: userId={} reason=ALREADY_ACTIVE traceId={}",
                    userId, MDC.get("traceId"));
            throw new AppException(ErrorType.USER_ACTIVE);
        }

        if (user.getStatus() != AccountStatus.BLOCKED) {
            log.warn("UNBLOCK_USER_FAILED: userId={} reason=INVALID_STATUS traceId={}",
                    userId, MDC.get("traceId"));
            throw new AppException(ErrorType.USER_INVALID_STATUS);
        }

        user.setStatus(AccountStatus.ACTIVE);

        log.info("UNBLOCK_USER_SUCCESS: userId={} traceId={}",
                userId, MDC.get("traceId"));
    }

    // ================== GET PATIENTS ==================

    @Transactional(readOnly = true)
    public Page<PatientSummary> getAllPatients(int page, int size) {

        log.debug("GET_PATIENTS_QUERY: page={} size={} traceId={}",
                page, size, MDC.get("traceId"));

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, USER_CREATED_AT));

        Page<PatientSummary> result = userRepository.findByRole(Role.PATIENT, pageable)
                .map(AdminService::mapToPatientResponse);

        log.debug("GET_PATIENTS_RESULT: count={} traceId={}",
                result.getNumberOfElements(), MDC.get("traceId"));

        return result;
    }

    // ================== GET DOCTORS ==================

    @Transactional(readOnly = true)
    public Page<DoctorResponse> getAllDoctors(int page, int size) {

        log.debug("GET_DOCTORS_QUERY: page={} size={} traceId={}",
                page, size, MDC.get("traceId"));

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, DOCTOR_USER_CREATED_AT));

        Page<DoctorResponse> result = doctorRepository.findAll(pageable)
                .map(AdminService::mapToDoctorResponse);

        log.debug("GET_DOCTORS_RESULT: count={} traceId={}",
                result.getNumberOfElements(), MDC.get("traceId"));

        return result;
    }

    // ================== MAPPERS ==================

    private static DoctorResponse mapToDoctorResponse(Doctor doctor) {
        return new DoctorResponse(
                doctor.getUserId(),
                doctor.getUser().getEmail(),
                doctor.getUser().getStatus()
        );
    }

    private static PatientSummary mapToPatientResponse(User patient) {
        return new PatientSummary(
                patient.getId(),
                patient.getName(),
                patient.getEmail()
        );
    }
}