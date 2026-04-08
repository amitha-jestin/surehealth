package com.sociolab.surehealth.dto;

import com.sociolab.surehealth.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(@NotNull AccountStatus newStatus) {}
