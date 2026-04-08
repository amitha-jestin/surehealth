package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.NotificationResponse;
import com.sociolab.surehealth.enums.CaseStatus;
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
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ================= SEND CASE NOTIFICATION ASYNC =================
    // Synchronous implementation used by callers that need to observe failures (e.g. Kafka consumer)
    @Override
    public void sendCaseNotificationSync(Long userId,
                                         String message,
                                         CaseStatus newStatus
                                         ) {
        log.debug("action=notification_send status=NOOP layer=service method=sendCaseNotificationSync userId={}", userId);

        // Try to resolve target user; allow null (notification will have null user) if not found
        var optionalUser = userRepository.findById(userId);
        var targetUser = optionalUser.orElse(null);

        Notification notification = Notification.builder()
                .user(targetUser)
                .message(message)
                .newStatus(newStatus)
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
            log.warn("action=notification_send status=FAILED userId={} reason=WEBSOCKET_FAILED message={}", userId, wsEx.getMessage(), wsEx);
        }

        log.debug("action=notification_send status=SUCCESS userId={} notificationId={}", userId, notification.getId());
    }

    @Async
    @Override
    public void sendCaseNotification(Long userId,
                                     String message,
                                     CaseStatus newStatus) {
        log.debug("action=notification_send status=NOOP layer=service method=sendCaseNotification userId={}", userId);

        // Async wrapper delegates to the synchronous implementation
        sendCaseNotificationSync(userId, message, newStatus);
    }

    // ================= GET READ NOTIFICATIONS (PAGED) =================
    @Override
    public Page<NotificationResponse> getNotificationsForCurrentUser(Long userId,Boolean isRead, int page, int size) {
        log.debug("action=notification_fetch status=NOOP layer=service method=getNotificationsForCurrentUser userId={} isRead={} page={} size={}",
                userId, isRead, page, size);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("action=notification_fetch status=FAILED userId={} reason=USER_NOT_FOUND", userId);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND, "User not found");
                });

        Pageable pageable = PageRequest.of(page, size);

        Page<Notification> notifications = (isRead == null)
                ? notificationRepository.findByUser_Id(userId, pageable)
                : notificationRepository.findByUser_IdAndReadStatus(userId, isRead, pageable);

        log.info("action=notification_fetch status=SUCCESS userId={} count={}", userId, notifications.getTotalElements());

        return notifications.map(this::mapToResponse);
    }


    // ================= HELPER: MAP TO RESPONSE =================
    private NotificationResponse mapToResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUser() != null ? notification.getUser().getId() : null,
                notification.getMessage(),
                notification.getNewStatus(),
                notification.isReadStatus(),
                notification.getCreatedAt()
        );
    }
}
