package org.prestiq.recommendation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.prestiq.recommendation.dto.response.PagedResponse;
import org.prestiq.recommendation.dto.response.ScholarshipRecommendationResponse;
import org.prestiq.recommendation.security.UserPrincipal;
import org.prestiq.recommendation.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/recommendations")
@Tag(name = "Internal Recommendation API", description = "Inter-service endpoints for internal microservices (Notification, Application)")
public class InternalRecommendationController {

    private final RecommendationService recommendationService;

    public InternalRecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @Operation(summary = "Internal recalculation trigger for a student",
            description = "Protected internal endpoint callable only with internal service credentials or ADMIN role")
    @PostMapping("/recalculate/{studentId}")
    public ResponseEntity<Void> recalculateInternal(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        verifyInternalAccess(principal);
        recommendationService.recalculateRecommendations(studentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get recommendations for a student internally",
            description = "Protected internal endpoint callable only with internal service credentials or ADMIN role")
    @GetMapping("/{studentId}")
    public ResponseEntity<PagedResponse<ScholarshipRecommendationResponse>> getRecommendationsInternal(
            @PathVariable UUID studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) BigDecimal minScore,
            @RequestParam(defaultValue = "true") Boolean includeNeedsVerification,
            @RequestParam(defaultValue = "false") boolean recalculate,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        verifyInternalAccess(principal);
        PagedResponse<ScholarshipRecommendationResponse> response = recommendationService.getRecommendations(
                studentId, page, size, minScore, includeNeedsVerification, recalculate
        );
        return ResponseEntity.ok(response);
    }

    private void verifyInternalAccess(UserPrincipal principal) {
        if (principal == null || (!principal.isInternal() && !principal.hasRole("ADMIN"))) {
            throw new AccessDeniedException("Access denied: Internal endpoint requires verified internal service credentials or ADMIN role");
        }
    }
}
