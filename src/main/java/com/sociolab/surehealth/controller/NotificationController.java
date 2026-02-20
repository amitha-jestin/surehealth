package com.sociolab.surehealth.controller;


import com.sociolab.surehealth.dto.NotificationResponse;
import com.sociolab.surehealth.service.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(@Min (0)@RequestParam(defaultValue = "0") int page,
                                                                             @Min(1)@Max (50)@RequestParam(defaultValue = "10") int size,
                                                                             Authentication authentication
    ) {
        String email = authentication.getName();

        return ResponseEntity.ok(notificationService.getUnreadNotificationsForCurrentUser(page,size,email));
    }

    @GetMapping("/read")
    public ResponseEntity<List<NotificationResponse>> getReadNotifications(@RequestParam(defaultValue = "0") int page,
                                                                           @RequestParam(defaultValue = "10") int size,
                                                                           Authentication authentication) {
        String email = authentication.getName();

        return ResponseEntity.ok(notificationService.getReadNotificationsForCurrentUser(page,size,email));
    }

}
