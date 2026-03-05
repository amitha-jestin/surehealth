package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.LoginRequest;
import com.sociolab.surehealth.dto.LoginResponse;
import com.sociolab.surehealth.service.AuthService;
import com.sociolab.surehealth.utils.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@Validated
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    // ================== LOGIN ==================
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<com.sociolab.surehealth.dto.ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("AUTH_ATTEMPT: login email={} traceId={}", request.email(), MDC.get("traceId"));

        LoginResponse response = authService.login(request);

        log.info("AUTH_SUCCESS: login email={} traceId={}", request.email(), MDC.get("traceId"));

        return ResponseEntity.ok(ResponseUtil.success(response));
    }

    // ================== LOGOUT ==================
    @Operation(summary = "User logout", description = "Invalidate current JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful")
    })
    @PostMapping("/logout")
    public ResponseEntity<com.sociolab.surehealth.dto.ApiResponse<Void>> logout(HttpServletRequest request) {

        log.info("AUTH_ATTEMPT: logout traceId={}", MDC.get("traceId"));

        authService.logout(request);

        log.info("AUTH_SUCCESS: logout traceId={}", MDC.get("traceId"));

        return ResponseEntity.ok(ResponseUtil.successMessage("Logout successful"));
    }
}