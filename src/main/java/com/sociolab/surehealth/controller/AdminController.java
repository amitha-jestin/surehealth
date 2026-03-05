package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.*;
import com.sociolab.surehealth.service.AdminService;
import com.sociolab.surehealth.utils.ResponseUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
@Slf4j
public class AdminController {

    private final AdminService adminService;

    // ================== APPROVE DOCTOR ==================
    @PatchMapping("/doctors/{doctorId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveDoctor(
            @PathVariable @Min(1) Long doctorId) {

        log.info("ADMIN_ACTION: approveDoctor doctorId={} traceId={}",
                doctorId, MDC.get("traceId"));

        adminService.approveDoctor(doctorId);

        log.info("ADMIN_ACTION_SUCCESS: approveDoctor doctorId={} traceId={}",
                doctorId, MDC.get("traceId"));

        return ResponseEntity.ok(
                ResponseUtil.successMessage("Doctor approved successfully")
        );
    }

    // ================== BLOCK USER ==================
    @PatchMapping("/users/{userId}/block")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @PathVariable @Min(1) Long userId) {

        log.info("ADMIN_ACTION: blockUser userId={} traceId={}",
                userId, MDC.get("traceId"));

        adminService.blockUser(userId);

        log.info("ADMIN_ACTION_SUCCESS: blockUser userId={} traceId={}",
                userId, MDC.get("traceId"));

        return ResponseEntity.ok(
                ResponseUtil.successMessage("User blocked successfully")
        );
    }

    // ================== UNBLOCK USER ==================
    @PatchMapping("/users/{userId}/unblock")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @PathVariable @Min(1) Long userId) {

        log.info("ADMIN_ACTION: unblockUser userId={} traceId={}",
                userId, MDC.get("traceId"));

        adminService.unblockUser(userId);

        log.info("ADMIN_ACTION_SUCCESS: unblockUser userId={} traceId={}",
                userId, MDC.get("traceId"));

        return ResponseEntity.ok(
                ResponseUtil.successMessage("User unblocked successfully")
        );
    }

    // ================== GET PATIENTS ==================
    @GetMapping("/patients")
    public ResponseEntity<PagedResponse<PatientSummary>> getAllPatients(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        log.debug("ADMIN_QUERY: getAllPatients page={} size={} traceId={}",
                page, size, MDC.get("traceId"));

        var pagedPatients = adminService.getAllPatients(page, size);

        return ResponseEntity.ok(ResponseUtil.paged(pagedPatients));
    }

    // ================== GET DOCTORS ==================
    @GetMapping("/doctors")
    public ResponseEntity<PagedResponse<DoctorResponse>> getAllDoctors(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        log.debug("ADMIN_QUERY: getAllDoctors page={} size={} traceId={}",
                page, size, MDC.get("traceId"));

        var pagedDoctors = adminService.getAllDoctors(page, size);

        return ResponseEntity.ok(ResponseUtil.paged(pagedDoctors));
    }
}