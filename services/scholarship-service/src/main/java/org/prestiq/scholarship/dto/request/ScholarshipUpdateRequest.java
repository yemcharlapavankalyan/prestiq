package org.prestiq.scholarship.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.prestiq.scholarship.domain.entity.AwardStructure;
import org.prestiq.scholarship.domain.entity.SourceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ScholarshipUpdateRequest(
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @Size(max = 200, message = "Slug must not exceed 200 characters")
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase alphanumeric with hyphens")
        String slug,

        @Size(max = 255, message = "Provider name must not exceed 255 characters")
        String providerName,

        String description,

        @Pattern(regexp = "^https?://.*", message = "Official website URL must be a valid HTTP/HTTPS URL")
        @Size(max = 1000, message = "Official website URL must not exceed 1000 characters")
        String officialWebsiteUrl,

        @Pattern(regexp = "^https?://.*", message = "Application URL must be a valid HTTP/HTTPS URL")
        @Size(max = 1000, message = "Application URL must not exceed 1000 characters")
        String applicationUrl,

        SourceType sourceType,

        @DecimalMin(value = "0.00", message = "Award amount minimum must be non-negative")
        @Digits(integer = 12, fraction = 2, message = "Award amount format must have at most 12 integer digits and 2 decimals")
        BigDecimal awardAmountMin,

        @DecimalMin(value = "0.00", message = "Award amount maximum must be non-negative")
        @Digits(integer = 12, fraction = 2, message = "Award amount format must have at most 12 integer digits and 2 decimals")
        BigDecimal awardAmountMax,

        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be a valid 3-letter ISO 4217 code")
        String currencyCode,

        AwardStructure awardStructure,

        Boolean isRenewable,

        LocalDate applicationStartDate,

        LocalDate applicationDeadline,

        @Size(max = 20, message = "Academic year must not exceed 20 characters")
        String academicYear,

        List<@Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be a 2-letter uppercase ISO code") String> eligibleCountries,

        List<String> eligibleStates,

        List<String> educationLevels,

        List<String> eligibleInstitutions,

        List<String> eligibleFieldsOfStudy,

        @DecimalMin(value = "0.00", message = "Minimum GPA must be at least 0.00")
        @Digits(integer = 2, fraction = 2, message = "GPA format must have at most 2 integer digits and 2 decimals")
        BigDecimal minGpa,

        @DecimalMin(value = "0.01", message = "GPA scale must be greater than zero")
        @Digits(integer = 2, fraction = 2, message = "GPA scale format must have at most 2 integer digits and 2 decimals")
        BigDecimal gpaScale,

        @DecimalMin(value = "0.00", message = "Maximum family income must be non-negative")
        @Digits(integer = 12, fraction = 2, message = "Maximum income format must have at most 12 integer digits and 2 decimals")
        BigDecimal maxFamilyIncome,

        Boolean firstGenOnly,

        Boolean veteranOnly,

        Boolean disabilityOnly,

        List<Map<String, Object>> eligibilityCriteriaSchema,

        @Size(max = 255, message = "Source name must not exceed 255 characters")
        String sourceName
) {
}
