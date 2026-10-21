package org.prestiq.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.prestiq.application.domain.entity.ApplicationStatus;
import org.prestiq.application.dto.request.CreateApplicationRequest;
import org.prestiq.application.dto.request.UpdateApplicationRequest;
import org.prestiq.application.dto.request.UpdateStatusRequest;
import org.prestiq.application.dto.response.ApplicationResponse;
import org.prestiq.application.dto.response.PagedResponse;
import org.prestiq.application.dto.response.StatusHistoryResponse;
import org.prestiq.application.security.UserPrincipal;
import org.prestiq.application.service.ApplicationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@Tag(name = "Scholarship Applications", description = "Endpoints for student scholarship application tracking, stage progression, and status audit history")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Operation(summary = "Start tracking a scholarship application",
            description = "Creates a new scholarship application record in PLANNING stage. Automatically captures verified scholarship snapshot and schedules deadline reminders.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Application created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Active application already exists for this scholarship",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateApplicationRequest request
    ) {
        verifyAuthenticated(principal);
        ApplicationResponse response = applicationService.createApplication(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List student applications",
            description = "Retrieves paginated applications for the authenticated student, with optional status filtering and sorting.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Applications retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<PagedResponse<ApplicationResponse>> getApplications(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Filter by application stage (PLANNING, IN_PROGRESS, READY_TO_SUBMIT, SUBMITTED, UNDER_REVIEW, AWARDED, REJECTED, WITHDRAWN)")
            @RequestParam(required = false) ApplicationStatus status,
            @Parameter(description = "Zero-indexed page number (default: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page (1-100, default: 20)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property and direction (e.g. submissionDeadline,asc or createdAt,desc)")
            @RequestParam(defaultValue = "submissionDeadline,asc") String sort
    ) {
        verifyAuthenticated(principal);
        Pageable pageable = parseSort(page, size, sort);
        PagedResponse<ApplicationResponse> response = applicationService.getApplications(principal.getId(), status, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get application details by ID",
            description = "Retrieves application details with student ownership validation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application details retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied: Cannot access other students' applications"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        verifyAuthenticated(principal);
        ApplicationResponse response = applicationService.getApplicationById(id, principal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update application progress and notes",
            description = "Updates checklist completion percentage (0-100), student notes, external portal reference, or deadline.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ApplicationResponse> updateApplication(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationRequest request
    ) {
        verifyAuthenticated(principal);
        ApplicationResponse response = applicationService.updateApplication(id, principal, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Change application status through validated lifecycle transition",
            description = "Transitions application to target status according to strict business state-machine rules. Invalid transitions return 409 Conflict.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Application not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition rejected")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApplicationResponse> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        verifyAuthenticated(principal);
        ApplicationResponse response = applicationService.updateApplicationStatus(id, principal, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Withdraw an application",
            description = "Voluntarily withdraws an application from the tracking lifecycle.")
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<ApplicationResponse> withdrawApplication(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam(required = false) String notes
    ) {
        verifyAuthenticated(principal);
        ApplicationResponse response = applicationService.withdrawApplication(id, principal, notes);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get application status transition history",
            description = "Retrieves complete chronological audit log of status transitions for an application.")
    @GetMapping("/{id}/history")
    public ResponseEntity<List<StatusHistoryResponse>> getStatusHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        verifyAuthenticated(principal);
        List<StatusHistoryResponse> history = applicationService.getStatusHistory(id, principal);
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "View upcoming active application deadlines",
            description = "Retrieves active applications sorted by upcoming submission deadline.")
    @GetMapping("/deadlines")
    public ResponseEntity<List<ApplicationResponse>> getUpcomingDeadlines(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Number of days ahead to look (default: 30, 0 for all upcoming)")
            @RequestParam(defaultValue = "30") int daysAhead
    ) {
        verifyAuthenticated(principal);
        List<ApplicationResponse> deadlines = applicationService.getUpcomingDeadlines(principal.getId(), daysAhead);
        return ResponseEntity.ok(deadlines);
    }

    private void verifyAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication required");
        }
    }

    private Pageable parseSort(int page, int size, String sortParam) {
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;

        Sort sort = Sort.by(Sort.Direction.ASC, "submissionDeadline");
        if (sortParam != null && !sortParam.isBlank()) {
            String[] parts = sortParam.split(",");
            String field = parts[0].trim();
            Sort.Direction direction = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc"))
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;

            // Restrict sortable fields
            if (field.equals("submissionDeadline") || field.equals("createdAt") || field.equals("updatedAt") || field.equals("currentStatus")) {
                sort = Sort.by(direction, field);
            }
        }
        return PageRequest.of(page, size, sort);
    }
}
