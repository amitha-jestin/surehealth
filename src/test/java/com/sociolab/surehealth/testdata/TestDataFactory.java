package com.sociolab.surehealth.testdata;

import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.model.Doctor;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.DoctorRepository;
import com.sociolab.surehealth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Test data factory to persist Users and Doctors for integration tests.
 * - Encodes passwords using the application's PasswordEncoder bean
 * - Persists Users first, then associated Doctor rows (which map PK to user id)
 */
@Component
public class TestDataFactory {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    public TestDataFactory(UserRepository userRepository,
                           DoctorRepository doctorRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createAdmin(String rawPassword) {
        User user = UserBuilder.aUser()
                .withRole(Role.ADMIN)
                .withPassword(rawPassword == null ? "adminpass" : rawPassword)
                .withEmail("admin@example.com")
                .withName("Admin User")
                .build();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(AccountStatus.ACTIVE);
        return userRepository.save(user);
    }

    @Transactional
    public User createPatient(String rawPassword) {
        User user = UserBuilder.aUser()
                .withRole(Role.PATIENT)
                .withPassword(rawPassword == null ? "patientpass" : rawPassword)
                .withEmail("patient" + UUID.randomUUID() + "@example.com")
                .withName("Patient User")
                .build();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(AccountStatus.ACTIVE);
        return userRepository.save(user);
    }

    @Transactional
    public Doctor createDoctor(String rawPassword) {
        // create user first
        User user = UserBuilder.aUser()
                .withRole(Role.DOCTOR)
                .withPassword(rawPassword == null ? "doctorpass" : rawPassword)
                .withEmail("doctor+" + UUID.randomUUID() + "@example.com")
                .withName("Doctor User")
                .build();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(AccountStatus.PENDING);
        User saved = userRepository.save(user);

        Doctor doctor = DoctorBuilder.aDoctor()
                .withUser(saved)
                .withLicenseNumber("LIC-TEST-" + System.currentTimeMillis())
                .build();
        // doctor.userId will be set via @MapsId when saving
        return doctorRepository.save(doctor);
    }

}

