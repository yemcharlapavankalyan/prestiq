package org.prestiq.application.controller;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@Tag(name = "Application Management", description = "Endpoints for student scholarship application tracking")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    private void checkAuth(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Full authentication is required");
        }
    }

    @Operation(summary = "Create an application for a scholarship")
    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(
            @Valid @RequestBody CreateApplicationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        checkAuth(principal);
        ApplicationResponse response = applicationService.createApplication(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get list of applications for the current student")
    @GetMapping
    public ResponseEntity<PagedResponse<ApplicationResponse>> getApplications(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "submissionDeadline,asc") String sort,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        checkAuth(principal);
        Sort s = Sort.by(Sort.Direction.ASC, "submissionDeadline");
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            Sort.Direction dir = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            s = Sort.by(dir, parts[0].trim());
        }
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), s);
        PagedResponse<ApplicationResponse> response = applicationService.getApplications(principal.getId(), status, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get application details by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        checkAuth(principal);
        ApplicationResponse response = applicationService.getApplicationById(id, principal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update application details (progress, notes)")
    @RequestMapping(value = "/{id}", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<ApplicationResponse> updateApplication(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        checkAuth(principal);
        ApplicationResponse response = applicationService.updateApplication(id, principal, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Transition application status")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApplicationResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        checkAuth(principal);
        ApplicationResponse response = applicationService.updateApplicationStatus(id, principal, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Withdraw an application")
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<ApplicationResponse> withdraw(
            @PathVariable UUID id,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        checkAuth(principal);
        ApplicationResponse response = applicationService.withdrawApplication(id, principal, notes);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get status change history for an application")
    @GetMapping("/{id}/history")
    public ResponseEntity<List<StatusHistoryResponse>> getHistory(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        checkAuth(principal);
        List<StatusHistoryResponse> response = applicationService.getStatusHistory(id, principal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get upcoming application deadlines")
    @GetMapping("/deadlines")
    public ResponseEntity<List<ApplicationResponse>> getUpcomingDeadlines(
            @RequestParam(defaultValue = "30") int daysAhead,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        checkAuth(principal);
        List<ApplicationResponse> response = applicationService.getUpcomingDeadlines(principal.getId(), daysAhead);
        return ResponseEntity.ok(response);
    }
}

