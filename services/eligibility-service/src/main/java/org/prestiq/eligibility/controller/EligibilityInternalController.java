package org.prestiq.eligibility.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.prestiq.eligibility.dto.request.BatchEvaluateRequest;
import org.prestiq.eligibility.dto.response.BatchEvaluateResponse;
import org.prestiq.eligibility.security.UserPrincipal;
import org.prestiq.eligibility.service.EligibilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/eligibility")
@Tag(name = "Eligibility Internal API", description = "Internal service-to-service endpoints for recommendation engine")
public class EligibilityInternalController {

    private final EligibilityService eligibilityService;

    public EligibilityInternalController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    @Operation(summary = "Batch evaluate student against multiple scholarships",
            description = "Protected internal endpoint callable only with internal service credentials or ADMIN role")
    @PostMapping("/batch-evaluate")
    public ResponseEntity<BatchEvaluateResponse> batchEvaluate(
            @Valid @RequestBody BatchEvaluateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null || (!principal.isInternal() && !principal.hasRole("ADMIN"))) {
            throw new AccessDeniedException("Access denied: Internal endpoint requires verified internal service credentials or ADMIN role");
        }
        BatchEvaluateResponse response = eligibilityService.batchEvaluate(request);
        return ResponseEntity.ok(response);
    }
}
