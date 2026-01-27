package com.sociolab.surehealth.dto;

import com.sociolab.surehealth.enums.AccountStatus;

public record DoctorResponse(Long id, String email, AccountStatus status) {
}
