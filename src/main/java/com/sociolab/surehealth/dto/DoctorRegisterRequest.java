package com.sociolab.surehealth.dto;

import com.sociolab.surehealth.enums.Speciality;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.awt.*;

@Getter
@Setter
@NoArgsConstructor
public class DoctorRegisterRequest {

    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    @Size(min = 8)
    private String password;

    @NotBlank
    private Speciality speciality;

    @NotBlank
    private String licenseNumber;
    @Min(0)
    private int experienceYears;


}
