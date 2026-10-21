package org.prestiq.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.prestiq.notification.domain.entity.Notification;
import org.prestiq.notification.domain.entity.NotificationType;
import org.prestiq.notification.dto.request.ApplicationStatusNotificationRequest;
import org.prestiq.notification.dto.request.CancelDeadlineReminderRequest;
import org.prestiq.notification.dto.request.ScheduleDeadlineReminderRequest;
import org.prestiq.notification.dto.response.NotificationResponse;
import org.prestiq.notification.dto.response.ScheduledReminderResponse;
import org.prestiq.notification.security.UserPrincipal;
import org.prestiq.notification.service.DeadlineReminderService;
import org.prestiq.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/internal/v1/notifications")
@Tag(name = "Internal Notification API", description = "Inter-service notification scheduling and trigger endpoints")
public class InternalNotificationController {

    private final DeadlineReminderService reminderService;
    private final NotificationService notificationService;

    public InternalNotificationController(
            DeadlineReminderService reminderService,
            NotificationService notificationService
    ) {
        this.reminderService = reminderService;
        this.notificationService = notificationService;
    }

    @Operation(summary = "Schedule deadline countdown reminders",
            description = "Protected internal endpoint callable only with internal service credentials or ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reminders scheduled successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping("/deadlines")
    public ResponseEntity<List<ScheduledReminderResponse>> scheduleDeadlineReminders(
            @Valid @RequestBody ScheduleDeadlineReminderRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        verifyInternalAccess(principal);
        List<ScheduledReminderResponse> response = reminderService.scheduleReminders(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Cancel pending deadline reminders for an application",
            description = "Protected internal endpoint called when an application is withdrawn or cancelled.")
    @PostMapping("/deadlines/cancel")
    public ResponseEntity<Map<String, Object>> cancelDeadlineReminders(
            @Valid @RequestBody CancelDeadlineReminderRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        verifyInternalAccess(principal);
        int count = reminderService.cancelReminders(request.applicationId());
        return ResponseEntity.ok(Map.of(
                "applicationId", request.applicationId(),
                "cancelledCount", count
        ));
    }

    @Operation(summary = "Dispatch application status update notification",
            description = "Protected internal endpoint called when application status transitions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Notification dispatched"),
            @ApiResponse(responseCode = "200", description = "Notification suppressed by preference or duplicate"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping("/application-status")
    public ResponseEntity<?> notifyApplicationStatusChange(
            @Valid @RequestBody ApplicationStatusNotificationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        verifyInternalAccess(principal);

        String scholarshipTitle = (request.scholarshipTitle() != null && !request.scholarshipTitle().isBlank())
                ? request.scholarshipTitle()
                : "Scholarship Application";

        String title = "Application Status: " + scholarshipTitle;
        String message = String.format("Your application status has been updated from %s to %s.",
                request.fromStatus(), request.toStatus());

        String idempotencyKey = (request.idempotencyKey() != null && !request.idempotencyKey().isBlank())
                ? request.idempotencyKey()
                : "APP_STATUS:" + request.applicationId() + ":" + request.toStatus();

        Optional<Notification> notificationOpt = notificationService.createNotification(
                request.studentId(),
                NotificationType.APPLICATION_STATUS_UPDATE,
                title,
                message,
                "APPLICATION",
                request.applicationId().toString(),
                idempotencyKey
        );

        if (notificationOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(NotificationResponse.fromEntity(notificationOpt.get()));
        } else {
            return ResponseEntity.ok(Map.of(
                    "status", "SKIPPED_OR_DUPLICATE",
                    "applicationId", request.applicationId()
            ));
        }
    }

    @Operation(summary = "Process due deadline reminders on demand",
            description = "Protected internal / operational trigger endpoint to execute deadline reminder countdown engine.")
    @PostMapping("/process-reminders")
    public ResponseEntity<Map<String, Object>> processReminders(
            @RequestParam(defaultValue = "50") int batchSize,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        verifyInternalAccess(principal);
        int processed = reminderService.processDueReminders(batchSize);
        return ResponseEntity.ok(Map.of("processedCount", processed));
    }

    private void verifyInternalAccess(UserPrincipal principal) {
        if (principal == null || (!principal.isInternal() && !principal.hasRole("ADMIN"))) {
            throw new AccessDeniedException("Access denied: Internal endpoint requires verified internal service credentials or ADMIN role");
        }
    }
}
