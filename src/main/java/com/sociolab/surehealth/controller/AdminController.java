package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.*;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.sociolab.surehealth.service.AdminService;
import com.sociolab.surehealth.utils.ResponseUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@Validated
@RequestMapping(value = "/api/v1/admin", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

    // ================== APPROVE DOCTOR ==================
    @Operation(summary = "Approve doctor account", description = "Admin approves a pending doctor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Doctor approved successfully"),
            @ApiResponse(responseCode = "404", description = "Doctor not found"),
            @ApiResponse(responseCode = "400", description = "Doctor already active or blocked")
    })
    @PatchMapping("/doctors/{doctorId}/approve")
    public ResponseEntity<BaseResponse<Void>> approveDoctor(
            @PathVariable @Min(1) Long doctorId,
            @AuthenticationPrincipal UserPrincipal adminPrincipal) {

        Long adminId = adminPrincipal.userId();
        log.info("action=admin_approve_doctor status=START adminId={} doctorId={}", adminId, doctorId);
        adminService.approveDoctor(adminId, doctorId);
        log.info("action=admin_approve_doctor status=SUCCESS adminId={} doctorId={}", adminId, doctorId);

        return ResponseEntity.ok(
                ResponseUtil.successMessage("Doctor approved successfully")
        );
    }

    // ================== CHANGE ACCOUNT STATUS ==================
    @Operation(summary = "Change user account status", description = "Admin changes a user's account status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User status changed successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid status or operation")
    })
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<BaseResponse<Void>> updateUserStatus(
            @PathVariable @Min(1) Long userId,
            @Valid @RequestBody UserStatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal adminPrincipal) {

        Long adminId = adminPrincipal.userId();

        log.info("action=admin_update_user_status status=START adminId={} userId={} newStatus={}",
                adminId, userId, request.newStatus());

        adminService.updateUserStatus(adminId, userId, request.newStatus());
        log.info("action=admin_update_user_status status=SUCCESS adminId={} userId={} newStatus={}",
                adminId, userId, request.newStatus());

        String message = switch (request.newStatus()) {
            case BLOCKED -> "User blocked successfully";
            case ACTIVE -> "User unblocked successfully";
            default -> "User status updated successfully";
        };

        return ResponseEntity.ok(ResponseUtil.successMessage(message));
    }

    // ================== GET PATIENTS ==================
    @Operation(summary = "Get all patients", description = "Admin retrieves paginated list of patients")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patients retrieved successfully")
    })
    @GetMapping("/patients")
    public ResponseEntity<PagedResponse<PatientSummary>> getAllPatients(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        log.info("action=admin_get_patients status=START page={} size={}", page, size);
        var pagedPatients = adminService.getAllPatients(page, size);
        log.info("action=admin_get_patients status=SUCCESS page={} size={} count={}",
                page, size, pagedPatients.getNumberOfElements());
        return ResponseEntity.ok(ResponseUtil.paged(pagedPatients));
    }

    // ================== GET DOCTORS ==================
    @Operation(summary = "Get all doctors", description = "Admin retrieves paginated list of doctors")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Doctors retrieved successfully")
    })
    @GetMapping("/doctors")
    public ResponseEntity<PagedResponse<DoctorResponse>> getAllDoctors(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        log.info("action=admin_get_doctors status=START page={} size={}", page, size);
        var pagedDoctors = adminService.getAllDoctors(page, size);
        log.info("action=admin_get_doctors status=SUCCESS page={} size={} count={}",
                page, size, pagedDoctors.getNumberOfElements());
        return ResponseEntity.ok(ResponseUtil.paged(pagedDoctors));
    }
}
