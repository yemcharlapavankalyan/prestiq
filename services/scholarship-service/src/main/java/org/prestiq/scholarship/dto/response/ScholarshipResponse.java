package org.prestiq.scholarship.dto.response;

import org.prestiq.scholarship.domain.entity.AwardStructure;
import org.prestiq.scholarship.domain.entity.PublicationStatus;
import org.prestiq.scholarship.domain.entity.Scholarship;
import org.prestiq.scholarship.domain.entity.SourceType;
import org.prestiq.scholarship.domain.entity.VerificationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ScholarshipResponse(
        Long id,
        String slug,
        String title,
        String providerName,
        String description,
        String officialWebsiteUrl,
        String applicationUrl,
        SourceType sourceType,
        BigDecimal awardAmountMin,
        BigDecimal awardAmountMax,
        String currencyCode,
        AwardStructure awardStructure,
        boolean isRenewable,
        LocalDate applicationStartDate,
        LocalDate applicationDeadline,
        boolean isExpired,
        String academicYear,
        List<String> eligibleCountries,
        List<String> eligibleStates,
        List<String> educationLevels,
        List<String> eligibleInstitutions,
        List<String> eligibleFieldsOfStudy,
        BigDecimal minGpa,
        BigDecimal gpaScale,
        BigDecimal maxFamilyIncome,
        boolean firstGenOnly,
        boolean veteranOnly,
        boolean disabilityOnly,
        List<Map<String, Object>> eligibilityCriteriaSchema,
        VerificationStatus verificationStatus,
        UUID verifiedByUserId,
        Instant verifiedAt,
        String rejectionReason,
        PublicationStatus publicationStatus,
        Instant publishedAt,
        Instant archivedAt,
        String archivedReason,
        String sourceName,
        Instant lastVerifiedAt,
        List<ScholarshipSourceResponse> sources,
        Instant createdAt,
        Instant updatedAt
) {
    public static ScholarshipResponse fromEntity(Scholarship s) {
        if (s == null) return null;
        List<ScholarshipSourceResponse> sourcesList = (s.getSources() != null)
                ? s.getSources().stream().map(ScholarshipSourceResponse::fromEntity).toList()
                : Collections.emptyList();

        return new ScholarshipResponse(
                s.getId(),
                s.getSlug(),
                s.getTitle(),
                s.getProviderName(),
                s.getDescription(),
                s.getOfficialWebsiteUrl(),
                s.getApplicationUrl(),
                s.getSourceType(),
                s.getAwardAmountMin(),
                s.getAwardAmountMax(),
                s.getCurrencyCode(),
                s.getAwardStructure(),
                s.isRenewable(),
                s.getApplicationStartDate(),
                s.getApplicationDeadline(),
                s.isExpired(),
                s.getAcademicYear(),
                s.getEligibleCountries(),
                s.getEligibleStates(),
                s.getEducationLevels(),
                s.getEligibleInstitutions(),
                s.getEligibleFieldsOfStudy(),
                s.getMinGpa(),
                s.getGpaScale(),
                s.getMaxFamilyIncome(),
                s.isFirstGenOnly(),
                s.isVeteranOnly(),
                s.isDisabilityOnly(),
                s.getEligibilityCriteriaSchema(),
                s.getVerificationStatus(),
                s.getVerifiedByUserId(),
                s.getVerifiedAt(),
                s.getRejectionReason(),
                s.getPublicationStatus(),
                s.getPublishedAt(),
                s.getArchivedAt(),
                s.getArchivedReason(),
                s.getSourceName(),
                s.getLastVerifiedAt(),
                sourcesList,
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
