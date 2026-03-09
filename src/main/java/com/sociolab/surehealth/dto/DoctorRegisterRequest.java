package com.sociolab.surehealth.dto;

import com.sociolab.surehealth.enums.Speciality;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DoctorRegisterRequest {

    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$",
            message = "Password must contain uppercase, lowercase, number, and special character"
    )
    private String password;

    @NotNull
    private Speciality speciality;

    @NotBlank
    private String licenseNumber;
    @Min(0)
    private int experienceYears;


}
