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
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final String USER_CREATED_AT = "createdAt";
    private static final String DOCTOR_USER_CREATED_AT = "user.createdAt";

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    // ================== APPROVE DOCTOR ==================

    @Transactional
    public void approveDoctor(Long doctorId) {

        Doctor doctor = doctorRepository.findByUserId(doctorId)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND));

        AccountStatus status = doctor.getUser().getStatus();

        if (status == AccountStatus.ACTIVE) {
            throw new AppException(ErrorType.USER_ACTIVE);
        }

        if (status == AccountStatus.BLOCKED) {
            throw new AppException(ErrorType.USER_BLOCKED);
        }

        doctor.getUser().setRole(Role.DOCTOR);
        doctor.getUser().setStatus(AccountStatus.ACTIVE);
    }

    // ================== BLOCK USER ==================

    @Transactional
    public void blockUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND));

        if (user.getStatus() == AccountStatus.BLOCKED) {
            throw new AppException(ErrorType.USER_BLOCKED);
        }

        user.setStatus(AccountStatus.BLOCKED);
    }

    // ================== UNBLOCK USER ==================

    @Transactional
    public void unblockUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND));

        if (user.getStatus() == AccountStatus.ACTIVE) {
            throw new AppException(ErrorType.USER_ACTIVE);
        }

        if (user.getStatus() != AccountStatus.BLOCKED) {
            throw new AppException(ErrorType.USER_INVALID_STATUS);
        }

        user.setStatus(AccountStatus.ACTIVE);
    }

    // ================== GET PATIENTS ==================

    @Transactional(readOnly = true)
    public Page<PatientSummary> getAllPatients(int page, int size) {

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, USER_CREATED_AT));

        return userRepository.findByRole(Role.PATIENT, pageable)
                .map(AdminService::mapToPatientResponse);
    }

    // ================== GET DOCTORS ==================

    @Transactional(readOnly = true)
    public Page<DoctorResponse> getAllDoctors(int page, int size) {

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, DOCTOR_USER_CREATED_AT));

        return doctorRepository.findAll(pageable)
                .map(AdminService::mapToDoctorResponse);
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