package org.prestiq.recommendation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.prestiq.recommendation.dto.request.UpdatePreferencesRequest;
import org.prestiq.recommendation.dto.response.PagedResponse;
import org.prestiq.recommendation.dto.response.ScholarshipRecommendationResponse;
import org.prestiq.recommendation.dto.response.StudentPreferenceResponse;
import org.prestiq.recommendation.security.UserPrincipal;
import org.prestiq.recommendation.service.RecommendationService;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recommendations")
@Tag(name = "Personalized Recommendations", description = "Endpoints for student personalized scholarship recommendations, ranking, and preferences")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @Operation(
            summary = "Get personalized scholarship recommendations",
            description = "Retrieves explainable, ranked scholarship recommendations for the authenticated student. Only verified and eligible scholarships are included."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Personalized recommendations retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid Bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cross-student access denied",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Student profile not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "Downstream service unavailable",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<PagedResponse<ScholarshipRecommendationResponse>> getRecommendations(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Optional student ID (Admin only)")
            @RequestParam(required = false) UUID studentId,
            @Parameter(description = "Minimum composite match score filter (0-100)")
            @RequestParam(required = false) BigDecimal minScore,
            @Parameter(description = "Include scholarships requiring verification (default: true)")
            @RequestParam(defaultValue = "true") Boolean includeNeedsVerification,
            @Parameter(description = "Force full recalculation across downstream services (default: false)")
            @RequestParam(defaultValue = "false") boolean recalculate,
            @Parameter(description = "Zero-indexed page number (default: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page (1-100, default: 20)")
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID targetStudentId = resolveTargetStudent(principal, studentId);
        PagedResponse<ScholarshipRecommendationResponse> response = recommendationService.getRecommendations(
                targetStudentId, page, size, minScore, includeNeedsVerification, recalculate
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Trigger recommendation recalculation",
            description = "Forces immediate recalculation of personalized recommendations against current active scholarships and student profile."
    )
    @PostMapping("/recalculate")
    public ResponseEntity<Void> recalculate(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID studentId
    ) {
        UUID targetStudentId = resolveTargetStudent(principal, studentId);
        recommendationService.recalculateRecommendations(targetStudentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Dismiss a scholarship recommendation",
            description = "Dismisses a recommended scholarship from active recommendation listings."
    )
    @PatchMapping("/{scholarshipId}/dismiss")
    public ResponseEntity<Void> dismissRecommendation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long scholarshipId,
            @RequestParam(required = false) UUID studentId
    ) {
        UUID targetStudentId = resolveTargetStudent(principal, studentId);
        recommendationService.dismissRecommendation(targetStudentId, scholarshipId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Restore a dismissed recommendation",
            description = "Restores a previously dismissed scholarship recommendation back to the active list."
    )
    @PatchMapping("/{scholarshipId}/undismiss")
    public ResponseEntity<Void> undismissRecommendation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long scholarshipId,
            @RequestParam(required = false) UUID studentId
    ) {
        UUID targetStudentId = resolveTargetStudent(principal, studentId);
        recommendationService.undismissRecommendation(targetStudentId, scholarshipId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get student recommendation preferences",
            description = "Retrieves student preference configuration used in academic affinity and award weighting."
    )
    @GetMapping("/preferences")
    public ResponseEntity<StudentPreferenceResponse> getPreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID studentId
    ) {
        UUID targetStudentId = resolveTargetStudent(principal, studentId);
        StudentPreferenceResponse response = recommendationService.getPreferences(targetStudentId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update student recommendation preferences",
            description = "Updates student preference configuration and recalculates active recommendations."
    )
    @PutMapping("/preferences")
    public ResponseEntity<StudentPreferenceResponse> updatePreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdatePreferencesRequest request,
            @RequestParam(required = false) UUID studentId
    ) {
        UUID targetStudentId = resolveTargetStudent(principal, studentId);
        StudentPreferenceResponse response = recommendationService.updatePreferences(targetStudentId, request);
        return ResponseEntity.ok(response);
    }

    private UUID resolveTargetStudent(UserPrincipal principal, UUID requestedStudentId) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication is required to access recommendations");
        }
        if (requestedStudentId == null) {
            if (principal.getId() == null) {
                throw new AccessDeniedException("User ID is missing from security principal");
            }
            return principal.getId();
        }
        if (!principal.getId().equals(requestedStudentId) && !principal.hasRole("ADMIN") && !principal.isInternal()) {
            throw new AccessDeniedException("Access denied: You are not authorized to view recommendations for other students");
        }
        return requestedStudentId;
    }
}
