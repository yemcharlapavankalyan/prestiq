package org.prestiq.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.prestiq.application.domain.entity.ApplicationStatus;
import org.prestiq.application.dto.response.ApplicationResponse;
import org.prestiq.application.dto.response.PagedResponse;
import org.prestiq.application.security.UserPrincipal;
import org.prestiq.application.service.ApplicationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/applications")
@Tag(name = "Internal Application API", description = "Inter-service endpoints for internal microservices (Notification, AI)")
public class InternalApplicationController {

    private final ApplicationService applicationService;

    public InternalApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Operation(summary = "Get application details by ID for internal services",
            description = "Protected internal endpoint callable only with internal service credentials or ADMIN role")
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        verifyInternalAccess(principal);
        ApplicationResponse response = applicationService.getApplicationById(id, principal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get applications for a student internally",
            description = "Protected internal endpoint callable only with internal service credentials or ADMIN role")
    @GetMapping("/student/{studentId}")
    public ResponseEntity<PagedResponse<ApplicationResponse>> getApplicationsForStudent(
            @PathVariable UUID studentId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        verifyInternalAccess(principal);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)),
                Sort.by(Sort.Direction.ASC, "submissionDeadline"));
        PagedResponse<ApplicationResponse> response = applicationService.getApplications(studentId, status, pageable);
        return ResponseEntity.ok(response);
    }

    private void verifyInternalAccess(UserPrincipal principal) {
        if (principal == null || (!principal.isInternal() && !principal.hasRole("ADMIN"))) {
            throw new AccessDeniedException("Access denied: Internal endpoint requires verified internal service credentials or ADMIN role");
        }
    }
}
