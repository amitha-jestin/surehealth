package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.DoctorRegisterRequest;
import com.sociolab.surehealth.dto.UserRegisterRequest;
import com.sociolab.surehealth.dto.UserRegisterResponse;
import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.logging.LogUtil;
import com.sociolab.surehealth.model.Doctor;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.DoctorRepository;
import com.sociolab.surehealth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DoctorRepository doctorRepository;

    /**
     * Register a new patient
     */
    public UserRegisterResponse registerPatient(UserRegisterRequest req) {
        log.info("Registering new patient email={} ", LogUtil.maskEmail(req.getEmail()));

        try {
            User user = new User();
            user.setName(req.getName());
            user.setEmail(req.getEmail());
            user.setRole(Role.PATIENT);
            user.setStatus(AccountStatus.ACTIVE);
            user.setPassword(passwordEncoder.encode(req.getPassword()));

            User savedUser = userRepository.save(user);
            log.info("Patient registered successfully userId={} ", savedUser.getId());

            return mapToUserRegisterResponse(savedUser, "Patient registered successfully");

        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate patient registration attempt");
            throw new AppException(ErrorType.DUPLICATE_RESOURCE, "Email already exists");
        }
    }

    /**
     * Register a new doctor
     */
    @Transactional
    public UserRegisterResponse registerDoctor(DoctorRegisterRequest req) {
        log.info("Registering new doctor email={}",
                LogUtil.maskEmail(req.getEmail()));

        try {
            // Create User entity
            User user = new User();
            user.setName(req.getName());
            user.setEmail(req.getEmail());
            user.setRole(Role.PENDING_DOCTOR);
            user.setStatus(AccountStatus.PENDING);
            user.setPassword(passwordEncoder.encode(req.getPassword()));

            User savedUser = userRepository.save(user);
            log.info("User entry for doctor created userId={}", savedUser.getId());

            // Create Doctor entity
            Doctor doctor = new Doctor();
            doctor.setUser(savedUser);
            doctor.setLicenseNumber(req.getLicenseNumber());
            doctor.setSpeciality(req.getSpeciality());
            doctor.setExperienceYears(req.getExperienceYears());
            doctor.setVerified(false);

            doctorRepository.save(doctor);
            log.info("Doctor registered successfully doctorId={}", doctor.getUser().getId());

            return mapToUserRegisterResponse(savedUser, "Doctor registered successfully. Awaiting admin approval");

        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate doctor registration attempt");
            throw new AppException(ErrorType.DUPLICATE_RESOURCE, "Email or license number already exists");
        }
    }


    /**
     * Map User entity to UserRegisterResponse DTO
     */
    private UserRegisterResponse mapToUserRegisterResponse(User user, String message) {
        return new UserRegisterResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                message
        );
    }
}