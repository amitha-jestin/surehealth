package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.*;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.logging.LogUtil;
import com.sociolab.surehealth.security.UserPrincipal;
import com.sociolab.surehealth.service.AuthService;
import com.sociolab.surehealth.utils.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

        log.info("action=auth_login status=START email={}", LogUtil.maskEmail(request.email()));

        LoginResponse response = authService.login(request);

        log.info("action=auth_login status=SUCCESS userId={}", response.id());

        return ResponseEntity.ok(ResponseUtil.success(response));
    }

    // ================== LOGOUT ==================
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("action=auth_logout status=START");

        if (userPrincipal == null) {
            throw new AppException(
                    ErrorType.UNAUTHORIZED,
                    "User authentication required"
            );
        }


        Long userId = userPrincipal.userId();
        String token = userPrincipal.accessToken();

        authService.logout(userId, token);

        log.info("action=auth_logout status=SUCCESS userId={}", userId);

        return ResponseEntity.ok(ResponseUtil.successMessage("Logout successful"));
    }

    // ================== REFRESH TOKEN ==================
    @Operation(summary = "Refresh access token", description = "Get new access and refresh tokens using existing refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tokens refreshed"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        log.info("action=auth_refresh status=START");

        // Extract refresh token from request body
        String refreshToken = request.refreshToken();

        // Call service to get new tokens
        RefreshTokenResponse refreshTokenResponse = authService.refreshAccessToken(refreshToken);

        log.info("action=auth_refresh status=SUCCESS");

        // Return both access and refresh tokens
        return ResponseEntity.ok(ResponseUtil.success(refreshTokenResponse));
    }
}
