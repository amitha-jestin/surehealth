package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.UserRegisterRequest;
import com.sociolab.surehealth.dto.UserRegisterResponse;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerPatient(@Valid @RequestBody UserRegisterRequest request) {
        User savedUser = userService.register(request);
        UserRegisterResponse response = new UserRegisterResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

}
