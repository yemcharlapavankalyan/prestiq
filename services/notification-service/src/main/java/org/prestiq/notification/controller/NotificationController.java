package org.prestiq.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.prestiq.notification.domain.entity.NotificationType;
import org.prestiq.notification.dto.response.MarkAllReadResponse;
import org.prestiq.notification.dto.response.NotificationResponse;
import org.prestiq.notification.dto.response.PagedResponse;
import org.prestiq.notification.dto.response.UnreadCountResponse;
import org.prestiq.notification.security.UserPrincipal;
import org.prestiq.notification.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications API", description = "In-app notifications for authenticated users")
@SecurityRequirement(name = "Bearer Authentication")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "Get user notifications",
            description = "Lists in-app notifications for the authenticated user, with optional filters for unread status and category.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<PagedResponse<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Filter only unread notifications")
            @RequestParam(required = false) Boolean unreadOnly,
            @Parameter(description = "Filter by notification type")
            @RequestParam(required = false) NotificationType type,
            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20, max: 100)")
            @RequestParam(defaultValue = "20") int size
    ) {
        verifyAuthenticated(principal);
        int pageIndex = Math.max(0, page);
        int pageSize = Math.min(100, Math.max(1, size));
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        PagedResponse<NotificationResponse> response = notificationService.getNotifications(
                principal.getId(),
                unreadOnly,
                type,
                pageable
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get unread notifications count",
            description = "Retrieves total number of unread notifications for the authenticated user.")
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        verifyAuthenticated(principal);
        UnreadCountResponse response = notificationService.getUnreadCount(principal.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Mark single notification as read",
            description = "Marks a specific notification as read, enforcing user ownership.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Notification not found or access denied")
    })
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        verifyAuthenticated(principal);
        NotificationResponse response = notificationService.markAsRead(id, principal.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Mark all notifications as read",
            description = "Marks all unread notifications as read for the authenticated user.")
    @PostMapping("/mark-all-read")
    public ResponseEntity<MarkAllReadResponse> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        verifyAuthenticated(principal);
        MarkAllReadResponse response = notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete or dismiss a notification",
            description = "Deletes a notification belonging to the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Notification deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Notification not found or access denied")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        verifyAuthenticated(principal);
        notificationService.deleteNotification(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    private void verifyAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication required");
        }
    }
}
