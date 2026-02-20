package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.NotificationResponse;
import com.sociolab.surehealth.enums.NotificationEventType;
import com.sociolab.surehealth.model.Notification;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.NotificationRepository;
import com.sociolab.surehealth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor

public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;


    @Async
    public void sendCaseNotification(Long userId,
                                     String message,
                                     NotificationEventType eventType) {


        Notification notification = Notification.builder()
                .userId(userId)
                .message(message)
                .eventType(eventType)
                .build();


        notificationRepository.save(notification);

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
        );

        // log.info("Notification saved for user {}", userId);

    }

    public List<NotificationResponse> getReadNotificationsForCurrentUser(int page, int size, String email) {

            Optional<User> user =  userRepository.findByEmail(email);// Placeholder for current user ID

            Page<Notification> notifications = notificationRepository.findByUserIdAndReadStatus(user.get().getId(), true, PageRequest.of(page, size));

            return notifications.stream()
                    .map(this::mapToResponse)
                    .toList();


    }

    public List<NotificationResponse> getUnreadNotificationsForCurrentUser(int page, int size, String email) {

            // For simplicity, we are not implementing user authentication here.
            // In a real application, you would get the current user's ID from the security context.
        Optional<User> user =  userRepository.findByEmail(email);// Placeholder for current user ID

        Page<Notification> notifications = notificationRepository.findByUserIdAndReadStatus(user.get().getId(), false, PageRequest.of(page, size));

            return notifications.stream()
                    .map(this::mapToResponse)
                    .toList();

}
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