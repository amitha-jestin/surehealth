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
import com.sociolab.surehealth.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @Operation(summary = "Get user notifications", description = "Retrieve a paginated list of the current user's notifications, with optional filtering by read/unread status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping()
    public ResponseEntity<PagedResponse<NotificationResponse>> getNotifications(
            @RequestParam(required = false) Boolean isRead,
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @Max(50) @RequestParam(defaultValue = "10") int size
    , @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.userId();
        log.info("action=notification_fetch status=START userId={} isRead={} page={} size={}",
                userId, isRead, page, size);

        Page<NotificationResponse> response =
                notificationService.getNotificationsForCurrentUser(userId, isRead, page, size);

        log.info("action=notification_fetch status=SUCCESS userId={} count={}",
                userId, response.getNumberOfElements());

        return ResponseEntity.ok(ResponseUtil.paged(response));
    }

}
