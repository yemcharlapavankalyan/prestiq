package org.prestiq.student.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.prestiq.student.dto.request.StudentProfileRequest;
import org.prestiq.student.dto.response.StudentProfileResponse;
import org.prestiq.student.security.UserPrincipal;
import org.prestiq.student.service.StudentProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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

import java.util.UUID;

@RestController
@RequestMapping({"/api/students", "/api/v1/students"})
@Tag(name = "Student Profile Management", description = "Endpoints for managing student academic qualifications and eligibility records")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    public StudentProfileController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    @Operation(summary = "Get current authenticated student's profile", description = "Retrieves the student profile belonging strictly to the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = StudentProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid Bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found for authenticated user",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping({"/me", "/me/profile"})
    public ResponseEntity<StudentProfileResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        StudentProfileResponse response = studentProfileService.getProfileByUserId(principal.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create or update current student's profile (Upsert)", description = "Creates or updates academic details and eligibility attributes for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile upserted successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = StudentProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid academic data",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping({"/me", "/me/profile"})
    public ResponseEntity<StudentProfileResponse> upsertMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StudentProfileRequest request
    ) {
        StudentProfileResponse response = studentProfileService.upsertProfile(principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Explicitly create student profile", description = "Creates a new student profile for the authenticated user. Rejects with 409 Conflict if one already exists.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Profile created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = StudentProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Profile already exists for user",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping({"/me", "/me/profile", ""})
    public ResponseEntity<StudentProfileResponse> createMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StudentProfileRequest request
    ) {
        StudentProfileResponse response = studentProfileService.createProfile(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get student profile by ID", description = "Retrieves student profile by profile UUID. Restricted to authorized administrators or the profile owner.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = StudentProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cross-user access prohibited",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping({"/{studentId}", "/{studentId}/profile"})
    public ResponseEntity<StudentProfileResponse> getProfileById(
            @PathVariable("studentId") UUID studentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        StudentProfileResponse response = studentProfileService.getProfileById(studentId, principal);
        return ResponseEntity.ok(response);
    }
}
