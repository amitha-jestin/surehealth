package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.*;
import com.sociolab.surehealth.model.Doctor;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.service.UserService;
import com.sociolab.surehealth.utils.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.net.URI;

@RestController
@RequestMapping(value = "/api/v1/users/register", produces = "application/json")
@Validated
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    // ================= REGISTER PATIENT =================
    @Operation(summary = "Register a new patient", description = "Create a new patient account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Patient registered successfully"),
            @ApiResponse(responseCode = "400", description = "Email already exists or validation error")
    })
    @PostMapping("/patient")
    public ResponseEntity<com.sociolab.surehealth.dto.ApiResponse<UserRegisterResponse>> registerPatient(
            @Valid @RequestBody UserRegisterRequest request
    ) {
        log.info("USER_REGISTER_ATTEMPT: role=PATIENT email={} traceId={}", request.getEmail(), MDC.get("traceId"));

        User patient = userService.registerPatient(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(patient.getId())
                .toUri();

        UserRegisterResponse response = new UserRegisterResponse(
                patient.getId(),
                patient.getName(),
                patient.getEmail(),
                patient.getRole().name(),
                "Patient registered successfully"
        );

        log.info("USER_REGISTER_SUCCESS: role=PATIENT email={} id={} traceId={}",
                patient.getEmail(), patient.getId(), MDC.get("traceId"));

        return ResponseEntity.created(location).body(ResponseUtil.success(response));
    }

    // ================= REGISTER DOCTOR =================
    @Operation(summary = "Register a new doctor", description = "Create a new doctor account (pending admin approval)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Doctor registered successfully, pending approval"),
            @ApiResponse(responseCode = "400", description = "Email or license already exists or validation error")
    })
    @PostMapping("/doctor")
    public ResponseEntity<com.sociolab.surehealth.dto.ApiResponse<UserRegisterResponse>> registerDoctor(
            @Valid @RequestBody DoctorRegisterRequest request
    ) {
        log.info("USER_REGISTER_ATTEMPT: role=DOCTOR email={} traceId={}", request.getEmail(), MDC.get("traceId"));

        Doctor doctor = userService.registerDoctor(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(doctor.getUser().getId())
                .toUri();

        UserRegisterResponse response = new UserRegisterResponse(
                doctor.getUser().getId(),
                doctor.getUser().getName(),
                doctor.getUser().getEmail(),
                doctor.getUser().getRole().name(),
                "Doctor registered successfully. Awaiting admin approval"
        );

        log.info("USER_REGISTER_SUCCESS: role=DOCTOR email={} id={} traceId={}",
                doctor.getUser().getEmail(), doctor.getUser().getId(), MDC.get("traceId"));

        return ResponseEntity.created(location).body(ResponseUtil.success(response));
    }
}