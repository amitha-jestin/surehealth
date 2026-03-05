package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.ApiResponse;
import com.sociolab.surehealth.dto.LoginRequest;
import com.sociolab.surehealth.dto.LoginResponse;
import com.sociolab.surehealth.service.AuthService;
import com.sociolab.surehealth.utils.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("AUTH_ATTEMPT: login email={} traceId={}",
                request.email(), MDC.get("traceId"));

        LoginResponse response = authService.login(request);

        log.info("AUTH_SUCCESS: login email={} traceId={}",
                request.email(), MDC.get("traceId"));

        return ResponseEntity.ok(ResponseUtil.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {

        // add user info to log later when we have user details in security context
        log.info("AUTH_ATTEMPT: logout traceId={}",
                 MDC.get("traceId"));

        authService.logout(request);

        log.info("AUTH_SUCCESS: logout traceId={}",
                 MDC.get("traceId"));

        return ResponseEntity.ok(ResponseUtil.success(null));
    }
}