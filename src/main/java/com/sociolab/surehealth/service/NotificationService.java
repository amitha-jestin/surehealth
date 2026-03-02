package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.NotificationResponse;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.enums.NotificationEventType;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.model.Notification;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.NotificationRepository;
import com.sociolab.surehealth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ================= SEND CASE NOTIFICATION ASYNC =================
    @Async
    public void sendCaseNotification(Long userId,
                                     String message,
                                     NotificationEventType eventType) {

        Notification notification = Notification.builder()
                .userId(userId)
                .message(message)
                .eventType(eventType)
                .readStatus(false)
                .build();

        notificationRepository.save(notification);

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
        );
    }

    // ================= GET READ NOTIFICATIONS (PAGED) =================
    public Page<NotificationResponse> getReadNotificationsForCurrentUser(String email, int page, int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND,"User not found"));

        Pageable pageable = PageRequest.of(page, size);

        Page<Notification> notifications = notificationRepository.findByUserIdAndReadStatus(user.getId(), true, pageable);

        return notifications.map(this::mapToResponse);
    }

    // ================= GET UNREAD NOTIFICATIONS (PAGED) =================
    public Page<NotificationResponse> getUnreadNotificationsForCurrentUser(String email, int page, int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND,"User not found"));

        Pageable pageable = PageRequest.of(page, size);

        Page<Notification> notifications = notificationRepository.findByUserIdAndReadStatus(user.getId(), false, pageable);

        return notifications.map(this::mapToResponse);
    }

    // ================= HELPER: MAP TO RESPONSE =================
    private NotificationResponse mapToResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getMessage(),
                notification.getEventType(),
                notification.isReadStatus(),
                notification.getCreatedAt()
        );
    }
}