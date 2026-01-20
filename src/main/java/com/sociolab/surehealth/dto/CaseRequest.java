package com.sociolab.surehealth.dto;

import com.sociolab.surehealth.enums.Speciality;
import com.sociolab.surehealth.enums.Urgency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CaseRequest {
    public String title;
    @NotBlank
    public String description;
    @NotNull
    private Speciality speciality;

    @NotNull
    private Urgency urgency;


}
