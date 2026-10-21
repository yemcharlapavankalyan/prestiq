package org.prestiq.recommendation.dto.response;

import org.prestiq.recommendation.domain.entity.StudentPreference;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentPreferenceResponse(
        UUID id,
        UUID studentId,
        List<String> preferredFields,
        List<String> preferredLocations,
        BigDecimal minAwardPreference,
        Instant updatedAt
) {
    public static StudentPreferenceResponse fromEntity(StudentPreference p) {
        if (p == null) return null;
        return new StudentPreferenceResponse(
                p.getId(),
                p.getStudentId(),
                p.getPreferredFields(),
                p.getPreferredLocations(),
                p.getMinAwardPreference(),
                p.getUpdatedAt()
        );
    }
}
