package org.prestiq.application.dto.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScholarshipClientDto(
        Long id,
        String slug,
        String title,
        String providerName,
        BigDecimal awardAmountMin,
        BigDecimal awardAmountMax,
        String currencyCode,
        LocalDate applicationDeadline,
        boolean isExpired,
        String verificationStatus,
        String publicationStatus
) {
}
