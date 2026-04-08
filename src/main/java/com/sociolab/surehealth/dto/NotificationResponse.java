package com.sociolab.surehealth.dto;

import com.sociolab.surehealth.enums.CaseStatus;
import com.sociolab.surehealth.enums.NotificationEventType;
import lombok.Data;

import java.time.LocalDateTime;


public record NotificationResponse(
        Long id,
        Long userId,
        String message,
        CaseStatus newStatus,
        boolean readStatus,
        LocalDateTime createdAt) {

}
