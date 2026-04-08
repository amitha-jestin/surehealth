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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    @DisplayName("shouldPersistAndSendNotification_whenUserExists")
    void shouldPersistAndSendNotification_whenUserExists() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        notificationService.sendCaseNotificationSync(1L, "Hello", CaseStatus.ASSIGNED);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals("Hello", saved.getMessage());
        assertEquals(CaseStatus.ASSIGNED, saved.getNewStatus());
        assertEquals(user, saved.getUser());
        verify(messagingTemplate).convertAndSendToUser(eq("1"), eq("/queue/notifications"), any(Notification.class));
    }

    @Test
    @DisplayName("shouldPersistNotification_whenWebsocketFails")
    void shouldPersistNotification_whenWebsocketFails() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("ws")).when(messagingTemplate)
                .convertAndSendToUser(eq("1"), eq("/queue/notifications"), any(Notification.class));

        assertDoesNotThrow(() ->
                notificationService.sendCaseNotificationSync(1L, "Hello", CaseStatus.ASSIGNED));

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("shouldPersistNotification_whenUserMissing")
    void shouldPersistNotification_whenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        notificationService.sendCaseNotificationSync(99L, "Hello", CaseStatus.ASSIGNED);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertNull(captor.getValue().getUser());
    }

    @Test
    @DisplayName("shouldThrowResourceNotFound_whenUserMissingForReadNotifications")
    void shouldThrowResourceNotFound_whenUserMissingForReadNotifications() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> notificationService.getNotificationsForCurrentUser(1L, true, 0, 10));

        assertEquals(ErrorType.RESOURCE_NOT_FOUND, ex.getErrorType());
    }

    @Test
    @DisplayName("shouldReturnReadNotifications_whenUserExists")
    void shouldReturnReadNotifications_whenUserExists() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        Notification notification = Notification.builder()
                .id(1L)
                .user(user)
                .message("Hello")
                .newStatus(CaseStatus.ASSIGNED)
                .readStatus(true)
                .createdAt(LocalDateTime.now())
                .build();
        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUser_IdAndReadStatus(eq(1L), eq(true), any(Pageable.class)))
                .thenReturn(page);

        Page<NotificationResponse> result =
                notificationService.getNotificationsForCurrentUser(1L, true,0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals(notification.getId(), result.getContent().get(0).id());
    }

    @Test
    @DisplayName("shouldReturnUnreadNotifications_whenUserExists")
    void shouldReturnUnreadNotifications_whenUserExists() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        Notification notification = Notification.builder()
                .id(2L)
                .user(user)
                .message("Hello")
                .newStatus(CaseStatus.ASSIGNED)
                .readStatus(false)
                .createdAt(LocalDateTime.now())
                .build();
        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUser_IdAndReadStatus(eq(1L), eq(false), any(Pageable.class)))
                .thenReturn(page);

        Page<NotificationResponse> result =
                notificationService.getNotificationsForCurrentUser( 1L,false, 0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals(notification.getId(), result.getContent().get(0).id());
    }
}
