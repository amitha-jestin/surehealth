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
    public void approveDoctor(Long adminId, Long doctorId) {
        log.info("admin_action=approve_doctor adminId={} doctorId={}" ,adminId, doctorId);

        Doctor doctor = doctorRepository.findByUserId(doctorId)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND));

        AccountStatus status = doctor.getUser().getStatus();

        if (status == AccountStatus.ACTIVE) {
            log.warn("admin_action_failed=approve_doctor adminId={} doctorId={} reason=ALREADY_ACTIVE", adminId, doctorId);
            throw new AppException(ErrorType.USER_ACTIVE);
        }

        if (status == AccountStatus.BLOCKED) {
            log.warn("admin_action_failed=approve_doctor adminId={} doctorId={} reason=BLOCKED", adminId, doctorId);
            throw new AppException(ErrorType.USER_BLOCKED);
        }

        doctor.getUser().setRole(Role.DOCTOR);
        doctor.getUser().setStatus(AccountStatus.ACTIVE);

        // Explicit save for clarity
        doctorRepository.save(doctor);

        log.info("admin_action_success=approve_doctor adminId={} doctorId={}", adminId, doctorId);

        // TODO: Add audit log entry here (adminId, action, targetId, entityType)
    }

    // ================== BLOCK USER ==================
    @Transactional
    public void blockUser(Long adminId, Long userId) {
        if (adminId.equals(userId)) {
            throw new AppException(ErrorType.INVALID_OPERATION, "Admin cannot block themselves");
        }

        log.info("admin_action=block_user adminId={} userId={}", adminId, userId);

        changeUserStatus(adminId, userId, AccountStatus.BLOCKED, "block");
    }

    // ================== UNBLOCK USER ==================
    @Transactional
    public void unblockUser(Long adminId, Long userId) {
        log.info("admin_action=unblock_user adminId={} userId={}", adminId, userId);

        changeUserStatus(adminId, userId, AccountStatus.ACTIVE, "unblock");
    }

    // ================== GET PATIENTS ==================
    @Transactional(readOnly = true)
    public Page<PatientSummary> getAllPatients(int page, int size) {
        log.debug("admin_query=get_all_patients page={} size={}", page, size);

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, USER_CREATED_AT));

        Page<PatientSummary> result = userRepository.findByRole(Role.PATIENT, pageable)
                .map(AdminService::mapToPatientResponse);

        log.debug("admin_query_result=get_all_patients count={}", result.getNumberOfElements());
        return result;
    }

    // ================== GET DOCTORS ==================
    @Transactional(readOnly = true)
    public Page<DoctorResponse> getAllDoctors(int page, int size) {
        log.debug("admin_query=get_all_doctors page={} size={}", page, size);

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, DOCTOR_USER_CREATED_AT));

        Page<DoctorResponse> result = doctorRepository.findAll(pageable)
                .map(AdminService::mapToDoctorResponse);

        log.debug("admin_query_result=get_all_doctors count={}", result.getNumberOfElements());
        return result;
    }

    // ================== PRIVATE HELPER ==================
    /**
     * Generic method to change user status with concurrency safety
     */
    private void changeUserStatus(Long adminId, Long userId, AccountStatus targetStatus, String action) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND));

        if (targetStatus == AccountStatus.BLOCKED) {
            if (user.getStatus() == AccountStatus.BLOCKED) {
                log.warn("admin_action_failed={} adminId={} userId={} reason=ALREADY_BLOCKED", action, adminId, userId);
                throw new AppException(ErrorType.USER_BLOCKED);
            }
        } else if (targetStatus == AccountStatus.ACTIVE) {
            if (user.getStatus() == AccountStatus.ACTIVE) {
                log.warn("admin_action_failed={} adminId={} userId={} reason=ALREADY_ACTIVE", action, adminId, userId);
                throw new AppException(ErrorType.USER_ACTIVE);
            }
            if (user.getStatus() != AccountStatus.BLOCKED) {
                log.warn("admin_action_failed={} adminId={} userId={} reason=INVALID_STATUS", action, adminId, userId);
                throw new AppException(ErrorType.USER_INVALID_STATUS);
            }
        }

        user.setStatus(targetStatus);
        userRepository.save(user);

        log.info("admin_action_success={} adminId={} userId={} newStatus={}", action, adminId, userId, targetStatus);
        // TODO: Add audit log entry here
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