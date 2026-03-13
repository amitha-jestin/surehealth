package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.*;
import com.sociolab.surehealth.service.NotificationService;
import com.sociolab.surehealth.utils.ResponseUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.sociolab.surehealth.security.SecurityUtil;
import com.sociolab.surehealth.logging.LogUtil;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping(value = "/api/v1/notifications", produces = "application/json")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    // ================= UNREAD =================
    @Operation(summary = "Get unread notifications", description = "Retrieve all unread notifications for the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Unread notifications retrieved successfully")
    })
    @GetMapping("/unread")
    public ResponseEntity<PagedResponse<NotificationResponse>> getUnreadNotifications(
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @Max(50) @RequestParam(defaultValue = "10") int size
    ) {
        String email = SecurityUtil.getCurrentUserEmail();
        String masked = LogUtil.maskEmail(email);
        log.debug("NOTIFICATION_QUERY: unreadNotifications email={} page={} size={}",
                masked, page, size);

        Page<NotificationResponse> response =
                notificationService.getUnreadNotificationsForCurrentUser(email, page, size);

        log.debug("NOTIFICATION_QUERY_SUCCESS: unreadNotifications email={} resultCount={}",
                masked, response.getNumberOfElements());

        return ResponseEntity.ok(ResponseUtil.paged(response));
    }

    // ================= READ =================
    @Operation(summary = "Get read notifications", description = "Retrieve all read notifications for the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Read notifications retrieved successfully")
    })
    @GetMapping("/read")
    public ResponseEntity<PagedResponse<NotificationResponse>> getReadNotifications(
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @Max(50) @RequestParam(defaultValue = "10") int size
    ) {
        String email = SecurityUtil.getCurrentUserEmail();
        String masked = LogUtil.maskEmail(email);
        log.debug("NOTIFICATION_QUERY: readNotifications email={} page={} size={}",
                masked, page, size);

        Page<NotificationResponse> response =
                notificationService.getReadNotificationsForCurrentUser(email, page, size);

        log.debug("NOTIFICATION_QUERY_SUCCESS: readNotifications email={} resultCount={}",
                masked, response.getNumberOfElements());

        return ResponseEntity.ok(ResponseUtil.paged(response));
    }
}