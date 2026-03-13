package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.BaseResponse;
import com.sociolab.surehealth.dto.LoginRequest;
import com.sociolab.surehealth.dto.LoginResponse;
import com.sociolab.surehealth.dto.RefreshTokenRequest;
import com.sociolab.surehealth.logging.LogUtil;
import com.sociolab.surehealth.service.AuthService;
import com.sociolab.surehealth.utils.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.Map;

@RestController
@Validated
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    // ================== LOGIN ==================
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("AUTH_ATTEMPT: login email={}", LogUtil.maskEmail(request.email()));

        LoginResponse response = authService.login(request);

        log.info("AUTH_SUCCESS: login id={}", response.id());

        return ResponseEntity.ok(ResponseUtil.success(response));
    }

    // ================== LOGOUT ==================
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(){
        log.info("AUTH_ATTEMPT: logout");

        authService.logout();

        log.info("AUTH_SUCCESS: logout");

        return ResponseEntity.ok(ResponseUtil.successMessage("Logout successful"));
    }

    // ================== REFRESH TOKEN ==================
    @Operation(summary = "Refresh access token", description = "Get new access and refresh tokens using existing refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tokens refreshed"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<Map<String, String>>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        log.info("AUTH_ATTEMPT: refresh token");

        // Extract refresh token from request body
        String refreshToken = request.refreshToken();

        // Call service to get new tokens
        Map<String, String> tokens = authService.refreshAccessToken(refreshToken);

        log.info("AUTH_SUCCESS: refresh token");

        // Return both access and refresh tokens
        return ResponseEntity.ok(ResponseUtil.success(tokens));
    }
}