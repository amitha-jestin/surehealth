package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.CaseRequest;
import com.sociolab.surehealth.dto.CaseResponse;
import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.enums.CaseStatus;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.exception.ResourceNotFoundException;
import com.sociolab.surehealth.model.MedicalCase;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.DoctorRepository;
import com.sociolab.surehealth.repository.MedicalCaseRepository;
import com.sociolab.surehealth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.sociolab.surehealth.model.Doctor;


import java.time.LocalDateTime;
import java.util.List;

import static java.util.Arrays.stream;

@Service
@RequiredArgsConstructor
public class CaseService {

    private final MedicalCaseRepository caseRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;




    public CaseResponse submitCase(String patientEmail, CaseRequest req) {

        User patient = userRepository.findByEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        Doctor doctor = doctorRepository.findByUserId(req.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if (doctor.getUser().getStatus() != AccountStatus.ACTIVE) {
            throw new ResourceNotFoundException("Doctor not available");
        }

        MedicalCase medicalCase = new MedicalCase();
        medicalCase.setTitle(req.getTitle());
        medicalCase.setDescription(req.getDescription());
        medicalCase.setSpeciality(req.getSpeciality());
        medicalCase.setUrgency(req.getUrgency());
        medicalCase.setPatientId(patient.getId());
        medicalCase.setDoctorId(req.getDoctorId());
        medicalCase.setStatus(CaseStatus.ASSIGNED);
        medicalCase.setCreatedAt(LocalDateTime.now());

        MedicalCase case1 = caseRepository.save(medicalCase);

        return mapToResponse(case1);
    }
    @Transactional
    public CaseResponse acceptCase(Long caseId, String doctorEmail) {
        MedicalCase medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found"));


        User user = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Doctor doctor = doctorRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        validateDoctorAction(medicalCase, doctor);


        medicalCase.setStatus(CaseStatus.ACCEPTED);



        return mapToResponse(medicalCase);
    }
    @Transactional
    public CaseResponse rejectCase(Long caseId, String doctorEmail) {
        MedicalCase medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found"));

        User user = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Doctor doctor = doctorRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        validateDoctorAction(medicalCase, doctor);

        medicalCase.setStatus(CaseStatus.REJECTED);
        return mapToResponse(medicalCase);
    }


    private CaseResponse mapToResponse(MedicalCase c) {
        return new CaseResponse(
                c.getId(),
                c.getTitle(),
                c.getDescription(),
                c.getSpeciality(),
                c.getUrgency(),
                c.getStatus(),
                c.getCreatedAt(),
                c.getDoctorId()
        );
    }

    private void validateDoctorAction(
            MedicalCase medicalCase,
            Doctor doctor
    ) {
        if (!medicalCase.getDoctorId().equals(doctor.getUser().getId())) {
            throw new IllegalStateException("You are not authorized to act on this case");
        }

        if (medicalCase.getStatus() != CaseStatus.ASSIGNED) {
            throw new IllegalStateException("Case already processed");
        }

        if (doctor.getUser().getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Doctor account is not active");
        }
    }


    public List<CaseResponse> getMyCases(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.DOCTOR && user.getRole() != com.sociolab.surehealth.enums.Role.PATIENT) {
            throw new IllegalStateException("Only doctors and patients can have cases");
        }

        List<MedicalCase> cases = switch (user.getRole()) {
            case DOCTOR -> caseRepository.findByDoctorId(user.getId());
            case PATIENT -> caseRepository.findByPatientId(user.getId());
            default -> throw new IllegalStateException("Invalid user role");
        };

        return cases.stream()
                .map(this::mapToResponse)
                .toList();



    }



}
