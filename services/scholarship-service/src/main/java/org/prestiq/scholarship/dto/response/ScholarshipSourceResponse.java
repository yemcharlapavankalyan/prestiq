package org.prestiq.scholarship.dto.response;

import org.prestiq.scholarship.domain.entity.ScholarshipSource;

import java.time.Instant;

public record ScholarshipSourceResponse(
        Long id,
        Long scholarshipId,
        String sourceUrl,
        String sourceName,
        String contentHash,
        Instant lastScrapedAt,
        int httpStatus,
        Instant createdAt
) {
    public static ScholarshipSourceResponse fromEntity(ScholarshipSource source) {
        if (source == null) return null;
        return new ScholarshipSourceResponse(
                source.getId(),
                source.getScholarship() != null ? source.getScholarship().getId() : null,
                source.getSourceUrl(),
                source.getSourceName(),
                source.getContentHash(),
                source.getLastScrapedAt(),
                source.getHttpStatus(),
                source.getCreatedAt()
        );
    }
}
