package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.BaseResponse;
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
@RequestMapping(value = "/api/v1/users", produces = "application/json")
@Validated
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

// ================= REGISTER USER (PATIENT OR DOCTOR) =================
    @Operation(summary = "Register a new user (patient or doctor)", description = "Create a new user account. Role is determined by the 'role' field in the request")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Email already exists or validation error")
    })
    @PostMapping
    public ResponseEntity<BaseResponse<UserRegisterResponse>> registerUser(
            @Valid @RequestBody UserRegisterRequest request
    ) {
        log.info("action=user_register status=START role={} email={}", request.getRole(), LogUtil.maskEmail(request.getEmail()));

        UserRegisterResponse user = userService.registerUser(request);

        URI location = createLocation(user);

        log.info("action=user_register status=SUCCESS role={} userId={}", user.role(), user.id());

        return ResponseEntity.created(location).body(ResponseUtil.success(user));
    }


    private URI createLocation(UserRegisterResponse user) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(user.id())
                .toUri();
    }

}
