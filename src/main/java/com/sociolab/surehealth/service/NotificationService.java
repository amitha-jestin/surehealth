package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.NotificationResponse;
import com.sociolab.surehealth.enums.CaseStatus;
import com.sociolab.surehealth.enums.NotificationEventType;
import org.springframework.data.domain.Page;

public interface NotificationService {

    void sendCaseNotificationSync(Long userId, String message, CaseStatus newStatus);

    void sendCaseNotification(Long userId, String message, CaseStatus newStatus);

    Page<NotificationResponse> getNotificationsForCurrentUser(Long userId, Boolean isRead, int page, int size);

}
