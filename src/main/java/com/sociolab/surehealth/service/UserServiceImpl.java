package com.sociolab.surehealth.service;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DoctorRepository doctorRepository;

    /**
     * Register a new user (patient or doctor based on role)
     */
    @Transactional
    @Override
    public UserRegisterResponse registerUser(UserRegisterRequest request) {
        log.debug("action=user_register status=NOOP layer=service method=registerUser role={}", request.getRole());

        try {
            // Step 1: Create base user
            User user = createBaseUser(request);

            User savedUser = userRepository.save(user);

            // Step 2: Role-specific logic
            switch (request.getRole()) {
                case PATIENT -> {
                    log.info("action=user_register status=SUCCESS userId={} role=PATIENT", savedUser.getId());
                    return mapToUserRegisterResponse(savedUser, "Patient registered successfully");
                }

                case DOCTOR -> {
                    createDoctorProfile(request, savedUser);

                    log.info("action=user_register status=SUCCESS userId={} role=PENDING_DOCTOR", savedUser.getId());
                    return mapToUserRegisterResponse(savedUser,
                            "Doctor registered successfully. Awaiting admin approval");
                }

                default -> throw new AppException(ErrorType.INVALID_REQUEST, "Invalid role");
            }

        } catch (DataIntegrityViolationException e) {
            log.warn("action=user_register status=FAILED reason=DUPLICATE email={}", LogUtil.maskEmail(request.getEmail()));
            throw new AppException(ErrorType.DUPLICATE_RESOURCE, "Email already exists");
        }
    }

    // ================= HELPER METHODS =================

    private User createBaseUser(UserRegisterRequest req) {
        User user = new User();

        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));

        if (req.getRole() == Role.DOCTOR) {
            user.setRole(Role.PENDING_DOCTOR);
            user.setStatus(AccountStatus.PENDING);
        } else {
            user.setRole(Role.PATIENT);
            user.setStatus(AccountStatus.ACTIVE);
        }

        return user;
    }

    private void createDoctorProfile(UserRegisterRequest req, User user) {

        // Validation for doctor-specific fields
        validateDoctorFields(req);

        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setLicenseNumber(req.getLicenseNumber());
        doctor.setSpeciality(req.getSpeciality());
        doctor.setExperienceYears(req.getExperienceYears());
        doctor.setVerified(false);

        doctorRepository.save(doctor);
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

    private void validateDoctorFields(UserRegisterRequest req) {
        if (req.getLicenseNumber() == null || req.getLicenseNumber().isBlank()) {
            throw new AppException(ErrorType.VALIDATION_ERROR, "License number is required");
        }

        if (req.getSpeciality() == null) {
            throw new AppException(ErrorType.VALIDATION_ERROR, "Speciality is required");
        }

        if (req.getExperienceYears() == null || req.getExperienceYears() < 0) {
            throw new AppException(ErrorType.VALIDATION_ERROR, "Valid experience is required");
        }

    }
}
