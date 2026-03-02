package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.ApiResponse;
import com.sociolab.surehealth.dto.DoctorResponse;
import com.sociolab.surehealth.dto.PagedResponse;
import com.sociolab.surehealth.dto.PatientSummary;
import com.sociolab.surehealth.service.AdminService;
import com.sociolab.surehealth.utils.ResponseUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping(value = "/api/v1/admin", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ================== APPROVE DOCTOR ==================

    @PatchMapping("/doctors/{doctorId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveDoctor(
            @PathVariable @Min(1) Long doctorId) {

        adminService.approveDoctor(doctorId);
        return ResponseEntity.ok(ResponseUtil.successMessage("Doctor approved successfully"));
    }

    // ================== BLOCK USER ==================

    @PatchMapping("/users/{userId}/block")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @PathVariable @Min(1) Long userId) {

        adminService.blockUser(userId);
        return ResponseEntity.ok(ResponseUtil.successMessage("User blocked successfully"));
    }

    // ================== UNBLOCK USER ==================

    @PatchMapping("/users/{userId}/unblock")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @PathVariable @Min(1) Long userId) {

        adminService.unblockUser(userId);
        return ResponseEntity.ok(ResponseUtil.successMessage("User unblocked successfully"));
    }

    // ================== GET PATIENTS ==================

    @GetMapping("/patients")
    public ResponseEntity<PagedResponse<PatientSummary>> getAllPatients(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return ResponseEntity.ok(
                ResponseUtil.paged(adminService.getAllPatients(page, size))
        );
    }

    // ================== GET DOCTORS ==================

    @GetMapping("/doctors")
    public ResponseEntity<PagedResponse<DoctorResponse>> getAllDoctors(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return ResponseEntity.ok(
                ResponseUtil.paged(adminService.getAllDoctors(page, size))
        );
    }
}