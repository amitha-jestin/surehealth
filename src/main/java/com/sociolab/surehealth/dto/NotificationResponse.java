package com.sociolab.surehealth.dto;

import com.sociolab.surehealth.enums.NotificationEventType;
import lombok.Data;

import java.time.LocalDateTime;


public record NotificationResponse(
        Long id,
        Long userId,
        String message,
        NotificationEventType eventType,
        boolean readStatus,
        LocalDateTime createdAt) {

}
