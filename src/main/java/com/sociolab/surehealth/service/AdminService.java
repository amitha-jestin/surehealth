package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.DoctorResponse;
import com.sociolab.surehealth.dto.PatientSummary;
import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.exception.ResourceNotFoundException;
import com.sociolab.surehealth.model.Doctor;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.DoctorRepository;
import com.sociolab.surehealth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AdminService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    @Transactional
    public void approveDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findByUserId(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if(doctor.getUser().getStatus() == AccountStatus.ACTIVE) {
            throw new IllegalStateException("Doctor is already approved");
        }
        if (doctor.getUser().getStatus() == AccountStatus.BLOCKED) {
            throw new IllegalStateException("Blocked doctor cannot be approved");
        }
        doctor.getUser().setStatus(AccountStatus.ACTIVE);
       // userRepository.save(doctor.getUser());
    }
    @Transactional
    public void blockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() == AccountStatus.BLOCKED) {
            throw new IllegalStateException("User is already blocked");
        }
        user.setStatus(AccountStatus.BLOCKED);
        //userRepository.save(user);
    }

    @Transactional
    public void unblockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if(user.getStatus() != AccountStatus.BLOCKED && user.getStatus() == AccountStatus.ACTIVE) {
            throw new IllegalStateException("User is already Active");
        }
        user.setStatus(AccountStatus.ACTIVE);
    }


    public Page<PatientSummary> getAllPatients(int page, int size) {

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return userRepository.findByRole(Role.PATIENT, pageable)
                .map(this::mapToPatientResponse);
    }

    public Page<DoctorResponse> getAllDoctors(int page, int size) {

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "user.createdAt"));

        return doctorRepository.findAll(pageable)
                .map(this::mapToDoctorResponse);


    }

    private DoctorResponse mapToDoctorResponse(Doctor doctor) {
        return new DoctorResponse(
                doctor.getUserId(),
                doctor.getUser().getEmail(),
                doctor.getUser().getStatus()
        );


    }

    private PatientSummary mapToPatientResponse(User patient) {
        return new PatientSummary(
                patient.getId(),
                patient.getName(),
                patient.getEmail()

        );
    }
}
