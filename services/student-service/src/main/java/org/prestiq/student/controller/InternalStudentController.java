package org.prestiq.student.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.prestiq.student.dto.response.StudentProfileResponse;
import org.prestiq.student.security.UserPrincipal;
import org.prestiq.student.service.StudentProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/students")
@Tag(name = "Internal Student API", description = "Inter-service endpoints for internal microservices (Eligibility, Recommendations)")
public class InternalStudentController {

    private final StudentProfileService studentProfileService;

    public InternalStudentController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    @Operation(summary = "Get student profile by user ID for internal services",
            description = "Protected internal endpoint callable only with internal service credentials or ADMIN role")
    @GetMapping("/{userId}/profile")
    public ResponseEntity<StudentProfileResponse> getProfileByUserId(
            @PathVariable("userId") UUID userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null || (!principal.isInternal() && !principal.hasRole("ADMIN"))) {
            throw new AccessDeniedException("Access denied: Internal endpoint requires verified internal service credentials or ADMIN role");
        }
        StudentProfileResponse response = studentProfileService.getProfileByUserId(userId);
        return ResponseEntity.ok(response);
    }
}
