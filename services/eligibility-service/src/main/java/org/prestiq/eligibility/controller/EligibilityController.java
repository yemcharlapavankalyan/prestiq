package org.prestiq.eligibility.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.prestiq.eligibility.dto.request.EvaluateStudentRequest;
import org.prestiq.eligibility.dto.response.EligibilityEvaluationResponse;
import org.prestiq.eligibility.security.UserPrincipal;
import org.prestiq.eligibility.service.EligibilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/eligibility")
@Tag(name = "Eligibility API", description = "Endpoints for evaluating and retrieving student eligibility results")
@SecurityRequirement(name = "bearerAuth")
public class EligibilityController {

    private final EligibilityService eligibilityService;

    public EligibilityController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    @Operation(summary = "Check eligibility against a scholarship",
            description = "Evaluates the authenticated student against the active rule set for the scholarship")
    @GetMapping("/check/{scholarshipId}")
    public ResponseEntity<EligibilityEvaluationResponse> checkEligibility(
            @PathVariable("scholarshipId") Long scholarshipId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication required to check scholarship eligibility.");
        }
        EligibilityEvaluationResponse response = eligibilityService.evaluateStudent(
                principal.getId(),
                scholarshipId,
                null
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "On-demand student eligibility evaluation",
            description = "Evaluates student eligibility with optional profile data for simulations")
    @PostMapping("/evaluate")
    public ResponseEntity<EligibilityEvaluationResponse> evaluate(
            @Valid @RequestBody EvaluateStudentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication required to evaluate eligibility.");
        }
        EligibilityEvaluationResponse response = eligibilityService.evaluateStudent(
                principal.getId(),
                request.scholarshipId(),
                request.studentProfile()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get evaluation result by ID",
            description = "Retrieves an evaluation result and full criterion breakdown. Cross-user protected.")
    @GetMapping("/evaluations/{id}")
    public ResponseEntity<EligibilityEvaluationResponse> getEvaluationById(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        EligibilityEvaluationResponse response = eligibilityService.getEvaluationById(id, principal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get student's evaluation history",
            description = "Lists all past eligibility evaluations computed for the authenticated student")
    @GetMapping("/my-evaluations")
    public ResponseEntity<List<EligibilityEvaluationResponse>> getMyEvaluations(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication required to view evaluation history.");
        }
        List<EligibilityEvaluationResponse> responses = eligibilityService.getMyEvaluations(principal.getId());
        return ResponseEntity.ok(responses);
    }
}
