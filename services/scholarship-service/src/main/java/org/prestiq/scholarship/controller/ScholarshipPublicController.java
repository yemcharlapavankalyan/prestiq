package org.prestiq.scholarship.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.prestiq.scholarship.domain.entity.SourceType;
import org.prestiq.scholarship.dto.response.PagedResponse;
import org.prestiq.scholarship.dto.response.ScholarshipResponse;
import org.prestiq.scholarship.dto.response.ScholarshipSummaryResponse;
import org.prestiq.scholarship.service.ScholarshipService;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/scholarships")
@Tag(name = "Public Scholarship Discovery", description = "Public read endpoints for discovering verified, published scholarship opportunities")
public class ScholarshipPublicController {

    private final ScholarshipService scholarshipService;

    public ScholarshipPublicController(ScholarshipService scholarshipService) {
        this.scholarshipService = scholarshipService;
    }

    @Operation(summary = "Search and list published scholarships",
            description = "Publicly discover verified, published scholarships with multi-criteria filtering, search, and pagination. Excludes drafts and archived records.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of scholarships retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter or sorting parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<PagedResponse<ScholarshipSummaryResponse>> getScholarships(
            @Parameter(description = "Keyword search in title, provider, or description")
            @RequestParam(required = false) String query,
            @Parameter(description = "Filter by education level (e.g., UNDERGRADUATE, POSTGRADUATE)")
            @RequestParam(required = false) String educationLevel,
            @Parameter(description = "Filter by eligible country (2-letter ISO code, e.g., US, IN)")
            @RequestParam(required = false) String country,
            @Parameter(description = "Minimum award amount threshold")
            @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(description = "Maximum award amount threshold")
            @RequestParam(required = false) BigDecimal maxAmount,
            @Parameter(description = "Filter by funding source type")
            @RequestParam(required = false) SourceType sourceType,
            @Parameter(description = "Include scholarships whose application deadline has passed (default: false)")
            @RequestParam(defaultValue = "false") boolean includeExpired,
            @Parameter(description = "Zero-indexed page number (default: 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page (1-100, default: 20)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property and direction (e.g., deadline,asc; amount,desc; title,asc). Allowed fields: deadline, amount, title, createdAt, provider")
            @RequestParam(defaultValue = "deadline,asc") String sort
    ) {
        PagedResponse<ScholarshipSummaryResponse> response = scholarshipService.searchPublishedScholarships(
                query, educationLevel, country, minAmount, maxAmount, sourceType, includeExpired, page, size, sort
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get published scholarship details by ID or slug",
            description = "Retrieves comprehensive scholarship details, active criteria, and verified provenance. Only published, verified scholarships are accessible.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Scholarship details retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ScholarshipResponse.class))),
            @ApiResponse(responseCode = "404", description = "Scholarship not found or not published",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{idOrSlug}")
    public ResponseEntity<ScholarshipResponse> getScholarshipByIdOrSlug(
            @Parameter(description = "Numeric ID or URL slug (e.g. 101 or stem-leaders-2026)")
            @PathVariable String idOrSlug
    ) {
        ScholarshipResponse response = scholarshipService.getPublishedScholarshipByIdOrSlug(idOrSlug);
        return ResponseEntity.ok(response);
    }
}
