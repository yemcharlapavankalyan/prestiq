package org.prestiq.recommendation.dto.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScholarshipSummaryClientDto(
        Long id,
        String slug,
        String title,
        String providerName,
        String sourceType,
        BigDecimal awardAmountMin,
        BigDecimal awardAmountMax,
        String currencyCode,
        String awardStructure,
        boolean isRenewable,
        LocalDate applicationStartDate,
        LocalDate applicationDeadline,
        boolean isExpired,
        List<String> eligibleCountries,
        List<String> educationLevels,
        BigDecimal minGpa,
        String verificationStatus,
        String publicationStatus
) {
}
