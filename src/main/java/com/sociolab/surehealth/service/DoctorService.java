package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.DoctorRegisterRequest;
import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.exception.DuplicateResourceException;
import com.sociolab.surehealth.model.Doctor;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.DoctorRepository;
import com.sociolab.surehealth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class DoctorService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    public Doctor registerDoctor(DoctorRegisterRequest request) {

        userRepository.findByEmail(request.getEmail())
                .ifPresent(u -> { throw new DuplicateResourceException("Email already exists"); });


        if (doctorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new DuplicateResourceException("License already registered");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.DOCTOR);
        user.setStatus(AccountStatus.PENDING);

        userRepository.save(user);

        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setLicenseNumber(request.getLicenseNumber());
        doctor.setSpeciality(request.getSpeciality());
        doctor.setExperienceYears(request.getExperienceYears());
        doctor.setVerified(false);


        doctorRepository.save(doctor);

        return doctor;
    }

}
