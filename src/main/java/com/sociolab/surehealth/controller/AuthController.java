package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.ApiResponse;
import com.sociolab.surehealth.dto.LoginRequest;
import com.sociolab.surehealth.dto.LoginResponse;
import com.sociolab.surehealth.service.AuthService;
import com.sociolab.surehealth.utils.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ResponseUtil.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {

        authService.logout(request);
        return ResponseEntity.ok(ResponseUtil.success(null));
    }
}