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
public class AdminServiceImpl implements AdminService {

    private static final String USER_CREATED_AT = "createdAt";
    private static final String DOCTOR_USER_CREATED_AT = "user.createdAt";

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    // ================== APPROVE DOCTOR ==================
    @Override
    @Transactional
    public void approveDoctor(Long adminId, Long doctorId) {
        log.debug("action=admin_approve_doctor status=NOOP layer=service method=approveDoctor adminId={} doctorId={}", adminId, doctorId);

        Doctor doctor = doctorRepository.findByUserId(doctorId)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND));

        AccountStatus status = doctor.getUser().getStatus();

        if (status == AccountStatus.ACTIVE) {
            log.info("action=admin_approve_doctor status=NOOP adminId={} doctorId={} reason=ALREADY_ACTIVE", adminId, doctorId);
            throw new AppException(ErrorType.USER_ACTIVE);
        }

        if (status == AccountStatus.BLOCKED) {
            log.warn("action=admin_approve_doctor status=FAILED adminId={} doctorId={} reason=BLOCKED", adminId, doctorId);
            throw new AppException(ErrorType.USER_BLOCKED);
        }

        doctor.getUser().setRole(Role.DOCTOR);
        doctor.getUser().setStatus(AccountStatus.ACTIVE);

        // Explicit save for clarity
        doctorRepository.save(doctor);

        log.info("action=admin_approve_doctor status=SUCCESS adminId={} doctorId={}", adminId, doctorId);

        // TODO: Add audit log entry here (adminId, action, targetId, entityType)
    }

    @Override
    @Transactional
    public void updateUserStatus(Long adminId, Long userId, AccountStatus targetStatus) {
        if (adminId.equals(userId)) {
            throw new AppException(ErrorType.INVALID_OPERATION, "Admin cannot change their own status");
        }
        if (targetStatus == null) {
            throw new AppException(ErrorType.VALIDATION_ERROR, "Target status is required");
        }
        log.debug("action=admin_update_user_status status=NOOP layer=service method=updateUserStatus adminId={} userId={} newStatus={}", adminId, userId, targetStatus);

        changeUserStatus(adminId, userId, targetStatus);
    }
    // ================== GET PATIENTS ==================
    @Override
    @Transactional(readOnly = true)
    public Page<PatientSummary> getAllPatients(int page, int size) {
        log.debug("action=admin_get_patients status=NOOP layer=service method=getAllPatients page={} size={}", page, size);

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, USER_CREATED_AT));

        Page<PatientSummary> result = userRepository.findByRole(Role.PATIENT, pageable)
                .map(AdminServiceImpl::mapToPatientResponse);

        log.info("action=admin_get_patients status=SUCCESS count={}", result.getNumberOfElements());
        return result;
    }

    // ================== GET DOCTORS ==================
    @Override
    @Transactional(readOnly = true)
    public Page<DoctorResponse> getAllDoctors(int page, int size) {
        log.debug("action=admin_get_doctors status=NOOP layer=service method=getAllDoctors page={} size={}", page, size);

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, DOCTOR_USER_CREATED_AT));

        Page<DoctorResponse> result = doctorRepository.findAll(pageable)
                .map(AdminServiceImpl::mapToDoctorResponse);

        log.info("action=admin_get_doctors status=SUCCESS count={}", result.getNumberOfElements());
        return result;
    }

    // ================== PRIVATE HELPER ==================
    /**
     * Generic method to change user status with concurrency safety
     */
    private void changeUserStatus(Long adminId, Long userId, AccountStatus targetStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND));

        if (user.getStatus() == targetStatus) {
            log.info("action=admin_update_user_status status=NOOP adminId={} userId={} reason=ALREADY_{}", adminId, userId, targetStatus);
            throw new AppException(ErrorType.INVALID_OPERATION , "User is already in the desired status");

        }

        if (targetStatus == AccountStatus.BLOCKED) {
            if (user.getStatus() == AccountStatus.BLOCKED) {
                log.info("action=admin_update_user_status status=NOOP adminId={} userId={} reason=ALREADY_BLOCKED", adminId, userId);
                throw new AppException(ErrorType.USER_BLOCKED);
            }
        } else if (targetStatus == AccountStatus.ACTIVE) {
            if (user.getStatus() == AccountStatus.ACTIVE) {
                log.info("action=admin_update_user_status status=NOOP adminId={} userId={} reason=ALREADY_ACTIVE", adminId, userId);
                throw new AppException(ErrorType.USER_ACTIVE);
            }
            if (user.getStatus() != AccountStatus.BLOCKED) {
                log.warn("action=admin_update_user_status status=FAILED adminId={} userId={} reason=INVALID_STATUS", adminId, userId);
                throw new AppException(ErrorType.USER_INVALID_STATUS);
            }
        }

        user.setStatus(targetStatus);
        userRepository.save(user);

        log.info("action=admin_update_user_status status=SUCCESS adminId={} userId={} newStatus={}", adminId, userId, targetStatus);
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
