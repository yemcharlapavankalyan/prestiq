package org.prestiq.scholarship.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.prestiq.scholarship.domain.entity.PublicationStatus;
import org.prestiq.scholarship.domain.entity.SourceType;
import org.prestiq.scholarship.domain.entity.VerificationStatus;
import org.prestiq.scholarship.dto.request.ScholarshipArchiveRequest;
import org.prestiq.scholarship.dto.request.ScholarshipCreateRequest;
import org.prestiq.scholarship.dto.request.ScholarshipRejectRequest;
import org.prestiq.scholarship.dto.request.ScholarshipSourceRequest;
import org.prestiq.scholarship.dto.request.ScholarshipUpdateRequest;
import org.prestiq.scholarship.dto.response.PagedResponse;
import org.prestiq.scholarship.dto.response.ScholarshipResponse;
import org.prestiq.scholarship.dto.response.ScholarshipSourceResponse;
import org.prestiq.scholarship.dto.response.ScholarshipSummaryResponse;
import org.prestiq.scholarship.security.UserPrincipal;
import org.prestiq.scholarship.service.ScholarshipService;
import org.prestiq.scholarship.service.ScholarshipSourceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/scholarships")
@Tag(name = "Administrative Scholarship Management", description = "Operations for scholarship ingestion, review, verification, publishing, and provenance")
@PreAuthorize("hasAnyRole('ADMIN', 'SCHOLARSHIP_OFFICER')")
public class ScholarshipAdminController {

    private final ScholarshipService scholarshipService;
    private final ScholarshipSourceService sourceService;

    public ScholarshipAdminController(ScholarshipService scholarshipService, ScholarshipSourceService sourceService) {
        this.scholarshipService = scholarshipService;
        this.sourceService = sourceService;
    }

    @Operation(summary = "Create a draft scholarship", description = "Creates a new scholarship in DRAFT status for review")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Draft created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ScholarshipResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or academic constraints violated",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate slug conflict",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<ScholarshipResponse> createDraft(
            @Valid @RequestBody ScholarshipCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ScholarshipResponse response = scholarshipService.createDraft(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List all scholarships (Admin view)", description = "Searches and filters all scholarships across draft, pending review, verified, published, and archived states")
    @GetMapping
    public ResponseEntity<PagedResponse<ScholarshipSummaryResponse>> listAdminScholarships(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) VerificationStatus verificationStatus,
            @RequestParam(required = false) PublicationStatus publicationStatus,
            @RequestParam(required = false) SourceType sourceType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        PagedResponse<ScholarshipSummaryResponse> response = scholarshipService.searchAdminScholarships(
                query, verificationStatus, publicationStatus, sourceType, page, size, sort
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get scholarship by ID (Admin view)", description = "Retrieves any scholarship by ID regardless of publication or verification status")
    @GetMapping("/{id}")
    public ResponseEntity<ScholarshipResponse> getScholarshipByIdAdmin(@PathVariable Long id) {
        ScholarshipResponse response = scholarshipService.getScholarshipByIdAdmin(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update scholarship details", description = "Updates details, eligibility criteria, or deadlines for an existing scholarship")
    @PutMapping("/{id}")
    public ResponseEntity<ScholarshipResponse> updateScholarship(
            @PathVariable Long id,
            @Valid @RequestBody ScholarshipUpdateRequest request
    ) {
        ScholarshipResponse response = scholarshipService.updateScholarship(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Submit scholarship for review", description = "Transitions status from DRAFT or REJECTED to PENDING_REVIEW")
    @PutMapping("/{id}/submit-review")
    public ResponseEntity<ScholarshipResponse> submitForReview(@PathVariable Long id) {
        ScholarshipResponse response = scholarshipService.submitForReview(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Verify scholarship", description = "Approves and certifies a scholarship opportunity, setting status to VERIFIED")
    @PutMapping("/{id}/verify")
    public ResponseEntity<ScholarshipResponse> verifyScholarship(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ScholarshipResponse response = scholarshipService.verifyScholarship(id, principal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reject scholarship", description = "Rejects a scholarship with an official reason. Unpublishes if currently live.")
    @PutMapping("/{id}/reject")
    public ResponseEntity<ScholarshipResponse> rejectScholarship(
            @PathVariable Long id,
            @Valid @RequestBody ScholarshipRejectRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ScholarshipResponse response = scholarshipService.rejectScholarship(id, request, principal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Publish scholarship", description = "Publishes a verified scholarship opportunity. Rejects if not VERIFIED.")
    @PutMapping("/{id}/publish")
    public ResponseEntity<ScholarshipResponse> publishScholarship(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ScholarshipResponse response = scholarshipService.publishScholarship(id, principal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Archive scholarship", description = "Archives a published scholarship record")
    @PutMapping("/{id}/archive")
    public ResponseEntity<ScholarshipResponse> archiveScholarship(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ScholarshipArchiveRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ScholarshipResponse response = scholarshipService.archiveScholarship(id, request, principal);
        return ResponseEntity.ok(response);
    }

    // Provenance Source Endpoints

    @Operation(summary = "Add provenance source", description = "Attaches an official scrape source, URL, and content hash to a scholarship")
    @PostMapping("/{id}/sources")
    public ResponseEntity<ScholarshipSourceResponse> addSource(
            @PathVariable Long id,
            @Valid @RequestBody ScholarshipSourceRequest request
    ) {
        ScholarshipSourceResponse response = sourceService.addSource(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List provenance sources", description = "Lists all registered provenance sources for a scholarship")
    @GetMapping("/{id}/sources")
    public ResponseEntity<List<ScholarshipSourceResponse>> listSources(@PathVariable Long id) {
        List<ScholarshipSourceResponse> response = sourceService.getSourcesByScholarshipId(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete provenance source", description = "Removes a provenance source from a scholarship")
    @DeleteMapping("/{id}/sources/{sourceId}")
    public ResponseEntity<Void> deleteSource(
            @PathVariable Long id,
            @PathVariable Long sourceId
    ) {
        sourceService.removeSource(id, sourceId);
        return ResponseEntity.noContent().build();
    }
}
