package org.prestiq.student.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.prestiq.student.dto.request.StudentProfileRequest;
import org.prestiq.student.dto.response.StudentProfileResponse;
import org.prestiq.student.exception.ProfileNotFoundException;
import org.prestiq.student.security.UserPrincipal;
import org.prestiq.student.service.StudentProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Student Profile API", description = "Student profile management endpoints")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    public StudentProfileController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    @Operation(summary = "Get current authenticated student profile")
    @GetMapping({"/me", "/profile"})
    public ResponseEntity<StudentProfileResponse> getCurrentProfile(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Full authentication is required");
        }
        try {
            StudentProfileResponse response = studentProfileService.getProfileByUserId(principal.getId());
            return ResponseEntity.ok(response);
        } catch (ProfileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(summary = "Create or upsert student profile")
    @PostMapping("/profile")
    public ResponseEntity<StudentProfileResponse> createOrUpsertProfile(
            @Valid @RequestBody StudentProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new AccessDeniedException("Full authentication is required");
        }
        StudentProfileResponse response = studentProfileService.upsertProfile(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update student profile")
    @PutMapping("/profile")
    public ResponseEntity<StudentProfileResponse> updateProfile(
            @Valid @RequestBody StudentProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new AccessDeniedException("Full authentication is required");
        }
        StudentProfileResponse response = studentProfileService.upsertProfile(principal.getId(), request);
        return ResponseEntity.ok(response);
    }
}

