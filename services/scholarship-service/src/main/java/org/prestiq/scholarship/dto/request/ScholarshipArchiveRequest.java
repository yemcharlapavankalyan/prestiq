package org.prestiq.scholarship.dto.request;

import jakarta.validation.constraints.Size;

public record ScholarshipArchiveRequest(
        @Size(max = 2000, message = "Archived reason must not exceed 2000 characters")
        String archivedReason
) {
}
