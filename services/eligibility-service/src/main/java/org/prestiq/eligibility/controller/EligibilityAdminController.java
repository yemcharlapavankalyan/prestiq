package org.prestiq.eligibility.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.prestiq.eligibility.dto.request.CreateRuleSetRequest;
import org.prestiq.eligibility.dto.response.RuleSetResponse;
import org.prestiq.eligibility.security.UserPrincipal;
import org.prestiq.eligibility.service.RuleSetAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/eligibility")
@Tag(name = "Eligibility Admin API", description = "Administrative endpoints for versioned rule set management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'SCHOLARSHIP_OFFICER')")
public class EligibilityAdminController {

    private final RuleSetAdminService adminService;

    public EligibilityAdminController(RuleSetAdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "Create scholarship eligibility rule set",
            description = "Creates a new versioned rule set with structured criteria for a scholarship")
    @PostMapping("/rulesets")
    public ResponseEntity<RuleSetResponse> createRuleSet(
            @Valid @RequestBody CreateRuleSetRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        RuleSetResponse response = adminService.createRuleSet(
                request,
                principal != null ? principal.getId() : null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Activate a rule set version",
            description = "Activates a rule set version and supersedes the currently active rule set for the scholarship")
    @PutMapping("/rulesets/{id}/activate")
    public ResponseEntity<RuleSetResponse> activateRuleSet(@PathVariable("id") Long id) {
        RuleSetResponse response = adminService.activateRuleSet(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get rule set by ID",
            description = "Retrieves a rule set and all contained criteria by rule set ID")
    @GetMapping("/rulesets/{id}")
    public ResponseEntity<RuleSetResponse> getRuleSetById(@PathVariable("id") Long id) {
        RuleSetResponse response = adminService.getRuleSetById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all rule sets for a scholarship",
            description = "Lists all historical and current rule set versions for a scholarship")
    @GetMapping("/rulesets/scholarship/{scholarshipId}")
    public ResponseEntity<List<RuleSetResponse>> getRuleSetsByScholarshipId(@PathVariable("scholarshipId") Long scholarshipId) {
        List<RuleSetResponse> responses = adminService.getRuleSetsByScholarshipId(scholarshipId);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Get active rule set for a scholarship",
            description = "Retrieves the currently active rule set version for a scholarship")
    @GetMapping("/rulesets/scholarship/{scholarshipId}/active")
    public ResponseEntity<RuleSetResponse> getActiveRuleSetForScholarship(@PathVariable("scholarshipId") Long scholarshipId) {
        RuleSetResponse response = adminService.getActiveRuleSetForScholarship(scholarshipId);
        return ResponseEntity.ok(response);
    }
}
