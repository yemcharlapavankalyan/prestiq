package org.prestiq.scholarship.dto.response;

import org.prestiq.scholarship.domain.entity.AwardStructure;
import org.prestiq.scholarship.domain.entity.PublicationStatus;
import org.prestiq.scholarship.domain.entity.Scholarship;
import org.prestiq.scholarship.domain.entity.SourceType;
import org.prestiq.scholarship.domain.entity.VerificationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ScholarshipSummaryResponse(
        Long id,
        String slug,
        String title,
        String providerName,
        SourceType sourceType,
        BigDecimal awardAmountMin,
        BigDecimal awardAmountMax,
        String currencyCode,
        AwardStructure awardStructure,
        boolean isRenewable,
        LocalDate applicationStartDate,
        LocalDate applicationDeadline,
        boolean isExpired,
        List<String> eligibleCountries,
        List<String> educationLevels,
        BigDecimal minGpa,
        VerificationStatus verificationStatus,
        PublicationStatus publicationStatus
) {
    public static ScholarshipSummaryResponse fromEntity(Scholarship s) {
        if (s == null) return null;
        return new ScholarshipSummaryResponse(
                s.getId(),
                s.getSlug(),
                s.getTitle(),
                s.getProviderName(),
                s.getSourceType(),
                s.getAwardAmountMin(),
                s.getAwardAmountMax(),
                s.getCurrencyCode(),
                s.getAwardStructure(),
                s.isRenewable(),
                s.getApplicationStartDate(),
                s.getApplicationDeadline(),
                s.isExpired(),
                s.getEligibleCountries(),
                s.getEducationLevels(),
                s.getMinGpa(),
                s.getVerificationStatus(),
                s.getPublicationStatus()
        );
    }
}
