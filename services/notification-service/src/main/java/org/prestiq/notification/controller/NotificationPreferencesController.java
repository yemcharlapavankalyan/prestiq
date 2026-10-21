package org.prestiq.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.prestiq.notification.dto.request.UpdatePreferencesRequest;
import org.prestiq.notification.dto.response.NotificationPreferencesResponse;
import org.prestiq.notification.security.UserPrincipal;
import org.prestiq.notification.service.NotificationPreferencesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
@Tag(name = "Notification Preferences API", description = "User notification categories and preferences management")
@SecurityRequirement(name = "Bearer Authentication")
public class NotificationPreferencesController {

    private final NotificationPreferencesService preferencesService;

    public NotificationPreferencesController(NotificationPreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    @Operation(summary = "Get user notification preferences",
            description = "Retrieves category preferences for the authenticated user, creating default preferences if none exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Preferences retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<NotificationPreferencesResponse> getPreferences(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        verifyAuthenticated(principal);
        NotificationPreferencesResponse response = preferencesService.getPreferences(principal.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update user notification preferences",
            description = "Updates notification categories (e.g. deadline reminders, status updates) for the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Preferences updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid preference payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping
    public ResponseEntity<NotificationPreferencesResponse> updatePreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdatePreferencesRequest request
    ) {
        verifyAuthenticated(principal);
        NotificationPreferencesResponse response = preferencesService.updatePreferences(principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    private void verifyAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication required");
        }
    }
}
