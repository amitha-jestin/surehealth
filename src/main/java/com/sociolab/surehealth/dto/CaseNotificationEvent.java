package com.sociolab.surehealth.dto;

import com.sociolab.surehealth.enums.CaseStatus;
import com.sociolab.surehealth.enums.NotificationEventType;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CaseNotificationEvent implements Serializable {

    private String eventId;
    private Long userId;
    private String message;
    private CaseStatus oldStatus;
    private CaseStatus newStatus;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
