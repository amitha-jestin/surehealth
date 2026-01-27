package com.sociolab.surehealth.dto;

import com.sociolab.surehealth.enums.Speciality;
import com.sociolab.surehealth.enums.Urgency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CaseRequest {
    @NotBlank
    @Size(max = 150)
    private String title;
    @NotBlank
    @Size(max = 2000)
    private String description;
    @NotNull
    private Speciality speciality;

    @NotNull
    private Urgency urgency;

    @NotBlank
    private long doctorId;





}
