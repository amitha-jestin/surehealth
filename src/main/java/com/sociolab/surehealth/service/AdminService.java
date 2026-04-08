package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.DoctorResponse;
import com.sociolab.surehealth.dto.PatientSummary;
import com.sociolab.surehealth.enums.AccountStatus;
import org.springframework.data.domain.Page;

public interface AdminService {

    // Approve a doctor account
    void approveDoctor(Long adminId, Long doctorId);

    // Change user status
    void updateUserStatus(Long adminId, Long userId, AccountStatus newStatus);

    // Get paginated list of patients
    Page<PatientSummary> getAllPatients(int page, int size);

    // Get paginated list of doctors
    Page<DoctorResponse> getAllDoctors(int page, int size);
}