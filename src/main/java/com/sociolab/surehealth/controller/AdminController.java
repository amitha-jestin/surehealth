package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.ApiResponse;
import com.sociolab.surehealth.dto.DoctorResponse;
import com.sociolab.surehealth.dto.PatientSummary;
import com.sociolab.surehealth.service.AdminService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/admin", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;


    @PatchMapping("/doctors/{doctorId}/approve")
    public ResponseEntity<ApiResponse> approveDoctor(@PathVariable Long doctorId) {
        adminService.approveDoctor(doctorId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(ApiResponse.success("Doctor approved successfully"));
    }

    @PatchMapping("/user/{userId}/block")
    public ResponseEntity<ApiResponse> blockUser(@PathVariable Long userId) {
        adminService.blockUser(userId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(ApiResponse.success("User blocked successfully"));
    }

    @PatchMapping("/user/{userId}/unblock")
    public ResponseEntity<ApiResponse> unblockUser(@PathVariable Long userId) {
        adminService.unblockUser(userId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(ApiResponse.success("User unblocked successfully"));
    }

    @GetMapping("/patients")
    public ResponseEntity<Page<PatientSummary>> getAllPatients(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(adminService.getAllPatients(page, size));
    }

    @GetMapping("/doctors")
    public ResponseEntity<Page<DoctorResponse>> getAllDoctors(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(adminService.getAllDoctors(page, size));
    }
}
