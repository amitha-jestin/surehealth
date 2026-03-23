package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.DoctorResponse;
import com.sociolab.surehealth.dto.PatientSummary;
import org.springframework.data.domain.Page;

public interface AdminService {

    // Approve a doctor account
    void approveDoctor(Long adminId, Long doctorId);

    // Block a user
    void blockUser(Long adminId, Long userId);

    // Unblock a user
    void unblockUser(Long adminId, Long userId);

    // Get paginated list of patients
    Page<PatientSummary> getAllPatients(int page, int size);

    // Get paginated list of doctors
    Page<DoctorResponse> getAllDoctors(int page, int size);
}