package org.prestiq.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.prestiq.application.dto.request.SaveScholarshipRequest;
import org.prestiq.application.dto.response.PagedResponse;
import org.prestiq.application.dto.response.SavedScholarshipResponse;
import org.prestiq.application.security.UserPrincipal;
import org.prestiq.application.service.SavedScholarshipService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/saved-scholarships")
@Tag(name = "Saved Scholarships", description = "Endpoints for students to bookmark, list, and manage saved scholarships")
public class SavedScholarshipController {

    private final SavedScholarshipService savedScholarshipService;

    public SavedScholarshipController(SavedScholarshipService savedScholarshipService) {
        this.savedScholarshipService = savedScholarshipService;
    }

    @Operation(summary = "Save or bookmark a scholarship",
            description = "Bookmarks a scholarship for future application tracking.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Scholarship saved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{scholarshipId}")
    public ResponseEntity<SavedScholarshipResponse> saveScholarship(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long scholarshipId,
            @RequestBody(required = false) SaveScholarshipRequest request
    ) {
        verifyAuthenticated(principal);
        SavedScholarshipResponse response = savedScholarshipService.saveScholarship(
                principal.getId(), scholarshipId, request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List student saved scholarships",
            description = "Retrieves paginated saved scholarships for the authenticated student.")
    @GetMapping
    public ResponseEntity<PagedResponse<SavedScholarshipResponse>> getSavedScholarships(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Zero-indexed page number (default: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page (1-100, default: 20)")
            @RequestParam(defaultValue = "20") int size
    ) {
        verifyAuthenticated(principal);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        PagedResponse<SavedScholarshipResponse> response = savedScholarshipService.getSavedScholarships(
                principal.getId(), pageable
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove a saved scholarship bookmark",
            description = "Removes a scholarship from the student's saved list.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Bookmark removed successfully"),
            @ApiResponse(responseCode = "404", description = "Bookmark not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{scholarshipId}")
    public ResponseEntity<Void> removeSavedScholarship(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long scholarshipId
    ) {
        verifyAuthenticated(principal);
        savedScholarshipService.removeSavedScholarship(principal.getId(), scholarshipId);
        return ResponseEntity.noContent().build();
    }

    private void verifyAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication required");
        }
    }
}
