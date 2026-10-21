package org.prestiq.scholarship.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ScholarshipRejectRequest(
        @NotBlank(message = "Rejection reason is required")
        @Size(max = 2000, message = "Rejection reason must not exceed 2000 characters")
        String rejectionReason
) {
}
