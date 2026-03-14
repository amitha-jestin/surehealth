package com.sociolab.surehealth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpinionRequest {

    @NotBlank(message = "Opinion text cannot be empty")
    @Size(max = 2000, message = "Opinion must not exceed 2000 characters")
    private String comment;

}
