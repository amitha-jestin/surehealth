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
    @Async
    public void sendCaseNotification(Long userId,
                                     String message,
                                     NotificationEventType eventType) {

        log.info("Sending notification userId={} eventType={} message={}", userId, eventType, message);

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

        log.debug("Notification sent successfully userId={} notificationId={}", userId, notification.getId());
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
                notificationRepository.findByUserIdAndReadStatus(user.getId(), true, pageable);

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
                notificationRepository.findByUserIdAndReadStatus(user.getId(), false, pageable);

        log.debug("Unread notifications fetched email={} total={}", masked, notifications.getTotalElements());

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