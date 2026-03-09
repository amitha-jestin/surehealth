package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.BaseResponse;
import com.sociolab.surehealth.dto.DoctorRegisterRequest;
import com.sociolab.surehealth.dto.UserRegisterRequest;
import com.sociolab.surehealth.dto.UserRegisterResponse;
import com.sociolab.surehealth.logging.LogUtil;
import com.sociolab.surehealth.service.UserService;
import com.sociolab.surehealth.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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
    public ResponseEntity<BaseResponse<UserRegisterResponse>> registerPatient(
            @Valid @RequestBody UserRegisterRequest request
    ) {
        log.info("USER_REGISTER_ATTEMPT: role=PATIENT email={}", LogUtil.maskEmail(request.getEmail()));

        UserRegisterResponse patient = userService.registerPatient(request);

        URI location = createLocation(patient);

        log.info("USER_REGISTER_SUCCESS: role=PATIENT id={}", patient.id());

        return ResponseEntity.created(location).body(ResponseUtil.success(patient));
    }

    // ================= REGISTER DOCTOR =================
    @Operation(summary = "Register a new doctor", description = "Create a new doctor account (pending admin approval)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Doctor registered successfully, pending approval"),
            @ApiResponse(responseCode = "400", description = "Email or license already exists or validation error")
    })
    @PostMapping("/doctor")
    public ResponseEntity<BaseResponse<UserRegisterResponse>> registerDoctor(
            @Valid @RequestBody DoctorRegisterRequest request
    ) {
        log.info("USER_REGISTER_ATTEMPT: role=DOCTOR email={}", LogUtil.maskEmail(request.getEmail()));

        UserRegisterResponse doctor = userService.registerDoctor(request);

        URI location = createLocation(doctor);

        log.info("USER_REGISTER_SUCCESS: role=DOCTOR id={}",
                 doctor.id());

        return ResponseEntity.created(location).body(ResponseUtil.success(doctor));
    }

    private URI createLocation(UserRegisterResponse user) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(user.id())
                .toUri();
    }

}