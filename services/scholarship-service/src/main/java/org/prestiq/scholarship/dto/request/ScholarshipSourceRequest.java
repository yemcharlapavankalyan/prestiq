package org.prestiq.scholarship.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ScholarshipSourceRequest(
        @NotBlank(message = "Source URL is required")
        @Pattern(regexp = "^https?://.*", message = "Source URL must be a valid HTTP/HTTPS URL")
        @Size(max = 1000, message = "Source URL must not exceed 1000 characters")
        String sourceUrl,

        @NotBlank(message = "Source name is required")
        @Size(max = 255, message = "Source name must not exceed 255 characters")
        String sourceName,

        @NotBlank(message = "Content hash is required")
        @Size(min = 64, max = 64, message = "Content hash must be a 64-character SHA-256 hexadecimal string")
        String contentHash,

        @Min(value = 100, message = "HTTP status code must be at least 100")
        @Max(value = 599, message = "HTTP status code must be at most 599")
        Integer httpStatus
) {
}
