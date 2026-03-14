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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.sociolab.surehealth.logging.LogUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ================= SEND CASE NOTIFICATION ASYNC =================
    // Synchronous implementation used by callers that need to observe failures (e.g. Kafka consumer)
    public void sendCaseNotificationSync(Long userId,
                                         String message,
                                         NotificationEventType eventType) {

        log.info("Sending notification (sync) userId={} eventType={} message={}", userId, eventType, message);

        // Try to resolve target user; allow null (notification will have null user) if not found
        var optionalUser = userRepository.findById(userId);
        var targetUser = optionalUser.orElse(null);

        Notification notification = Notification.builder()
                .user(targetUser)
                .message(message)
                .eventType(eventType)
                .readStatus(false)
                .build();

        notificationRepository.save(notification);

        // Send via websocket (if user is connected). This may throw runtime exceptions which callers can handle.
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    notification
            );
        } catch (Exception wsEx) {
            // Log websocket delivery failures but do not fail the entire notification persist step
            log.warn("Websocket delivery failed for userId={} error={}", userId, wsEx.getMessage(), wsEx);
        }

        log.debug("Notification persisted successfully userId={} notificationId={}", userId, notification.getId());
    }

    @Async
    public void sendCaseNotification(Long userId,
                                     String message,
                                     NotificationEventType eventType) {
        // Async wrapper delegates to the synchronous implementation
        sendCaseNotificationSync(userId, message, eventType);
    }

    // ================= GET READ NOTIFICATIONS (PAGED) =================
    public Page<NotificationResponse> getReadNotificationsForCurrentUser(String email, int page, int size) {
        String masked = LogUtil.maskEmail(email);
        log.debug("Fetching read notifications email={} page={} size={}", masked, page, size);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found for fetching read notifications email={}", masked);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND, "User not found");
                });

        Pageable pageable = PageRequest.of(page, size);

        Page<Notification> notifications =
                notificationRepository.findByUser_IdAndReadStatus(user.getId(), true, pageable);

        log.debug("Read notifications fetched email={} total={}", masked, notifications.getTotalElements());

        return notifications.map(this::mapToResponse);
    }

    // ================= GET UNREAD NOTIFICATIONS (PAGED) =================
    public Page<NotificationResponse> getUnreadNotificationsForCurrentUser(String email, int page, int size) {
        String masked = LogUtil.maskEmail(email);
        log.debug("Fetching unread notifications email={} page={} size={}", masked, page, size);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found for fetching unread notifications email={}", masked);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND, "User not found");
                });

        Pageable pageable = PageRequest.of(page, size);

        Page<Notification> notifications =
                notificationRepository.findByUser_IdAndReadStatus(user.getId(), false, pageable);

        log.debug("Unread notifications fetched email={} total={}", masked, notifications.getTotalElements());

        return notifications.map(this::mapToResponse);
    }

    // ================= HELPER: MAP TO RESPONSE =================
    private NotificationResponse mapToResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUser() != null ? notification.getUser().getId() : null,
                notification.getMessage(),
                notification.getEventType(),
                notification.isReadStatus(),
                notification.getCreatedAt()
        );
    }
}