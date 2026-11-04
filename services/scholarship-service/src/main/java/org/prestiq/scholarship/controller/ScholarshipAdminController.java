package org.prestiq.scholarship.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.prestiq.scholarship.domain.entity.PublicationStatus;
import org.prestiq.scholarship.domain.entity.SourceType;
import org.prestiq.scholarship.domain.entity.VerificationStatus;
import org.prestiq.scholarship.dto.request.ScholarshipArchiveRequest;
import org.prestiq.scholarship.dto.request.ScholarshipCreateRequest;
import org.prestiq.scholarship.dto.request.ScholarshipRejectRequest;
import org.prestiq.scholarship.dto.request.ScholarshipUpdateRequest;
import org.prestiq.scholarship.dto.response.PagedResponse;
import org.prestiq.scholarship.dto.response.ScholarshipResponse;
import org.prestiq.scholarship.dto.response.ScholarshipSummaryResponse;
import org.prestiq.scholarship.security.UserPrincipal;
import org.prestiq.scholarship.service.ScholarshipService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/scholarships")
@Tag(name = "Admin Scholarship Management", description = "Endpoints for review, verification, creation, and moderation")
public class ScholarshipAdminController {

    private final ScholarshipService scholarshipService;

    public ScholarshipAdminController(ScholarshipService scholarshipService) {
        this.scholarshipService = scholarshipService;
    }

    private void checkAdminAccess(UserPrincipal principal) {
        if (principal == null || (!principal.hasRole("ROLE_ADMIN") && !principal.hasRole("ROLE_SCHOLARSHIP_OFFICER") && !principal.isInternal())) {
            throw new AccessDeniedException("Administrative access required");
        }
    }

    @Operation(summary = "Search and list scholarships for admin dashboard")
    @GetMapping
    public ResponseEntity<PagedResponse<ScholarshipSummaryResponse>> listScholarships(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) VerificationStatus verificationStatus,
            @RequestParam(required = false) PublicationStatus publicationStatus,
            @RequestParam(required = false) SourceType sourceType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        checkAdminAccess(principal);
        PagedResponse<ScholarshipSummaryResponse> response = scholarshipService.searchAdminScholarships(
                query, verificationStatus, publicationStatus, sourceType, page, size, sort
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get scholarship by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ScholarshipResponse> getScholarship(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        checkAdminAccess(principal);
        ScholarshipResponse response = scholarshipService.getScholarshipByIdAdmin(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create a scholarship opportunity")
    @PostMapping
    public ResponseEntity<ScholarshipResponse> createScholarship(
            @Valid @RequestBody ScholarshipCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        checkAdminAccess(principal);
        ScholarshipResponse response = scholarshipService.createDraft(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update a scholarship opportunity")
    @PutMapping("/{id}")
    public ResponseEntity<ScholarshipResponse> updateScholarship(
            @PathVariable Long id,
            @Valid @RequestBody ScholarshipUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        checkAdminAccess(principal);
        ScholarshipResponse response = scholarshipService.updateScholarship(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Verify / Approve a scholarship opportunity")
    @RequestMapping(value = {"/{id}/verify", "/{id}/approve"}, method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ScholarshipResponse> verifyScholarship(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        checkAdminAccess(principal);
        ScholarshipResponse response = scholarshipService.verifyScholarship(id, principal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reject a scholarship opportunity")
    @RequestMapping(value = "/{id}/reject", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ScholarshipResponse> rejectScholarship(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ScholarshipRejectRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        checkAdminAccess(principal);
        ScholarshipRejectRequest rejectReq = request != null ? request : new ScholarshipRejectRequest("Rejected during administrative review.");
        ScholarshipResponse response = scholarshipService.rejectScholarship(id, rejectReq, principal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Publish a scholarship opportunity")
    @PutMapping("/{id}/publish")
    public ResponseEntity<ScholarshipResponse> publishScholarship(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        checkAdminAccess(principal);
        ScholarshipResponse response = scholarshipService.publishScholarship(id, principal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Archive a scholarship opportunity")
    @PutMapping("/{id}/archive")
    public ResponseEntity<ScholarshipResponse> archiveScholarship(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ScholarshipArchiveRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        checkAdminAccess(principal);
        ScholarshipResponse response = scholarshipService.archiveScholarship(id, request, principal);
        return ResponseEntity.ok(response);
    }
}
