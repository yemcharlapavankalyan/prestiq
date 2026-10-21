package org.prestiq.application.dto.response;

import org.prestiq.application.domain.entity.SavedScholarship;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SavedScholarshipResponse(
        Long id,
        UUID studentId,
        Long scholarshipId,
        String scholarshipTitle,
        String scholarshipProvider,
        LocalDate applicationDeadline,
        BigDecimal awardAmountMax,
        String notes,
        Instant createdAt
) {
    public static SavedScholarshipResponse fromEntity(SavedScholarship s) {
        if (s == null) return null;
        return new SavedScholarshipResponse(
                s.getId(),
                s.getStudentId(),
                s.getScholarshipId(),
                s.getScholarshipTitle(),
                s.getScholarshipProvider(),
                s.getApplicationDeadline(),
                s.getAwardAmountMax(),
                s.getNotes(),
                s.getCreatedAt()
        );
    }
}
