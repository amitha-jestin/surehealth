package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.DoctorRegisterRequest;
import com.sociolab.surehealth.dto.UserRegisterRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DoctorRepository doctorRepository;

    public User registerPatient(UserRegisterRequest req) {
        log.info("Registering new patient with email={}", req.getEmail());

        userRepository.findByEmail(req.getEmail())
                .ifPresent(u -> {
                    log.warn("Duplicate patient email attempted: {}", req.getEmail());
                    throw new AppException(ErrorType.DUPLICATE_RESOURCE, "Email already exists");
                });

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setRole(Role.PATIENT);
        user.setStatus(AccountStatus.ACTIVE);
        user.setPassword(passwordEncoder.encode(req.getPassword()));

        User savedUser = userRepository.save(user);
        log.info("Patient registered successfully userId={}", savedUser.getId());

        return savedUser;
    }

    public Doctor registerDoctor(DoctorRegisterRequest req) {
        log.info("Registering new doctor with email={} licenseNumber={}", req.getEmail(), req.getLicenseNumber());

        userRepository.findByEmail(req.getEmail())
                .ifPresent(u -> {
                    log.warn("Duplicate doctor email attempted: {}", req.getEmail());
                    throw new AppException(ErrorType.DUPLICATE_RESOURCE, "Email already exists");
                });

        if (doctorRepository.existsByLicenseNumber(req.getLicenseNumber())) {
            log.warn("Duplicate doctor license attempted: {}", req.getLicenseNumber());
            throw new AppException(ErrorType.DUPLICATE_RESOURCE, "License number already exists");
        }

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setRole(Role.PENDING_DOCTOR);
        user.setStatus(AccountStatus.PENDING);
        user.setPassword(passwordEncoder.encode(req.getPassword()));

        User savedUser = userRepository.save(user);
        log.info("User entry for doctor created userId={}", savedUser.getId());

        Doctor doctor = new Doctor();
        doctor.setUser(savedUser);
        doctor.setLicenseNumber(req.getLicenseNumber());
        doctor.setSpeciality(req.getSpeciality());
        doctor.setExperienceYears(req.getExperienceYears());
        doctor.setVerified(false);

        Doctor savedDoctor = doctorRepository.save(doctor);
        log.info("Doctor registered successfully doctorId={}", savedDoctor.getUserId());

        return savedDoctor;
    }

    public User getUser(Long id) {
        log.info("Fetching user with id={}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found id={}", id);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND, "User not found");
                });
    }
}