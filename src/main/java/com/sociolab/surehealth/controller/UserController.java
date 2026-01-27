package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.DoctorRegisterRequest;
import com.sociolab.surehealth.dto.UserRegisterRequest;
import com.sociolab.surehealth.dto.UserRegisterResponse;
import com.sociolab.surehealth.model.Doctor;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.service.DoctorService;
import com.sociolab.surehealth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/users")
@Validated
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final DoctorService doctorService;

    @PostMapping("/patient/register")
    public ResponseEntity<UserRegisterResponse> registerPatient(@Valid @RequestBody UserRegisterRequest request) {
        User patient = userService.register(request);


        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
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

        return ResponseEntity.created(location).body(response);

    }

    @PostMapping("/doctor/register")
    public ResponseEntity<UserRegisterResponse> registerDoctor(@Valid @RequestBody DoctorRegisterRequest request) {
        Doctor doctor = doctorService.registerDoctor(request);


        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(doctor.getUser().getId())
                .toUri();


        UserRegisterResponse response = new UserRegisterResponse(
                doctor.getUser().getId(),
                doctor.getUser().getName(),
                doctor.getUser().getEmail(),
                doctor.getUser().getRole().name(),
                "Doctor registered successfully. Awaiting admin approval");

        return ResponseEntity.created(location).body(response);


    }

}
