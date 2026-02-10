package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.DoctorRegisterRequest;
import com.sociolab.surehealth.dto.UserRegisterRequest;
import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.exception.DuplicateResourceException;
import com.sociolab.surehealth.model.Doctor;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.DoctorRepository;
import com.sociolab.surehealth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DoctorRepository doctorRepository;



    public User registerPatient(UserRegisterRequest req) {

        userRepository.findByEmail(req.getEmail())
                .ifPresent(u -> { throw new DuplicateResourceException("Email already exists"); });

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setRole(Role.PATIENT); // default
        user.setStatus(AccountStatus.ACTIVE); // default
        user.setPassword(passwordEncoder.encode(req.getPassword()));

        return userRepository.save(user);
    }

    public Doctor registerDoctor(DoctorRegisterRequest req) {

        userRepository.findByEmail(req.getEmail())
                .ifPresent(u -> { throw new DuplicateResourceException("Email already exists"); });

        if (doctorRepository.existsByLicenseNumber(req.getLicenseNumber())) {
            throw new DuplicateResourceException("License already registered");
        }
        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setRole(Role.PENDING_DOCTOR); // default
        user.setStatus(AccountStatus.PENDING); // default
        user.setPassword(passwordEncoder.encode(req.getPassword()));

        userRepository.save(user);

        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setLicenseNumber(req.getLicenseNumber());
        doctor.setSpeciality(req.getSpeciality());
        doctor.setExperienceYears(req.getExperienceYears());
        doctor.setVerified(false);

        doctorRepository.save(doctor);

        return doctor;
    }

    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }



}
