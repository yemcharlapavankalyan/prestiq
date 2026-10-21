package org.prestiq.application.dto.response;

import org.prestiq.application.domain.entity.ApplicationStatus;
import org.prestiq.application.domain.entity.ScholarshipApplication;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        UUID studentId,
        Long scholarshipId,
        String scholarshipTitle,
        String scholarshipProvider,
        ApplicationStatus currentStatus,
        String externalPortalReference,
        LocalDate submissionDeadline,
        LocalDate actualSubmissionDate,
        Instant submittedAt,
        BigDecimal awardAmountReceived,
        int progressPercentage,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static ApplicationResponse fromEntity(ScholarshipApplication app) {
        if (app == null) return null;
        return new ApplicationResponse(
                app.getId(),
                app.getStudentId(),
                app.getScholarshipId(),
                app.getScholarshipTitle(),
                app.getScholarshipProvider(),
                app.getCurrentStatus(),
                app.getExternalPortalReference(),
                app.getSubmissionDeadline(),
                app.getActualSubmissionDate(),
                app.getSubmittedAt(),
                app.getAwardAmountReceived(),
                app.getProgressPercentage(),
                app.getNotes(),
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }
}
