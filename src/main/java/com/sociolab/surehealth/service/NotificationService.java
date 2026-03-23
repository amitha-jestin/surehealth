package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.NotificationResponse;
import com.sociolab.surehealth.enums.NotificationEventType;
import org.springframework.data.domain.Page;

public interface NotificationService {

    void sendCaseNotificationSync(Long userId, String message, NotificationEventType eventType);

    void sendCaseNotification(Long userId, String message, NotificationEventType eventType);

    Page<NotificationResponse> getReadNotificationsForCurrentUser(String email, int page, int size);

    Page<NotificationResponse> getUnreadNotificationsForCurrentUser(String email, int page, int size);
}
