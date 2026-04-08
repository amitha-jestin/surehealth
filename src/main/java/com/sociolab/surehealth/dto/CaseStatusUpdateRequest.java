package com.sociolab.surehealth.dto;

import com.sociolab.surehealth.enums.CaseStatus;
import jakarta.validation.constraints.NotNull;

public record CaseStatusUpdateRequest(@NotNull(message = "Status is required") CaseStatus status) {
}
