package com.sociolab.surehealth.dto;

import com.sociolab.surehealth.enums.NotificationEventType;
import lombok.*;

import java.io.Serializable;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CaseNotificationEvent implements Serializable {

    private Long userId;
    private String message;
    private NotificationEventType eventType;

}
