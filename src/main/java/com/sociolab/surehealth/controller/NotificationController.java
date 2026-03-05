package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.*;
import com.sociolab.surehealth.service.NotificationService;
import com.sociolab.surehealth.utils.ResponseUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    // ================= UNREAD =================
    @GetMapping("/unread")
    public ResponseEntity<PagedResponse<NotificationResponse>> getUnreadNotifications(
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @Max(50) @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        String email = authentication.getName();
        log.debug("NOTIFICATION_QUERY: unreadNotifications email={} page={} size={} traceId={}",
                email, page, size, MDC.get("traceId"));

        Page<NotificationResponse> response =
                notificationService.getUnreadNotificationsForCurrentUser(email, page, size);

        log.debug("NOTIFICATION_QUERY_SUCCESS: unreadNotifications email={} resultCount={} traceId={}",
                email, response.getNumberOfElements(), MDC.get("traceId"));

        return ResponseEntity.ok(ResponseUtil.paged(response));
    }

    // ================= READ =================
    @GetMapping("/read")
    public ResponseEntity<PagedResponse<NotificationResponse>> getReadNotifications(
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @Max(50) @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        String email = authentication.getName();
        log.debug("NOTIFICATION_QUERY: readNotifications email={} page={} size={} traceId={}",
                email, page, size, MDC.get("traceId"));

        Page<NotificationResponse> response =
                notificationService.getReadNotificationsForCurrentUser(email, page, size);

        log.debug("NOTIFICATION_QUERY_SUCCESS: readNotifications email={} resultCount={} traceId={}",
                email, response.getNumberOfElements(), MDC.get("traceId"));

        return ResponseEntity.ok(ResponseUtil.paged(response));
    }
}