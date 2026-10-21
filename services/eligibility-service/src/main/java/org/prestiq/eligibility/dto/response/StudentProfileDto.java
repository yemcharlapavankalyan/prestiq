package org.prestiq.eligibility.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record StudentProfileDto(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String fullName,
        LocalDate dateOfBirth,
        String citizenshipCountry,
        String stateProvince,
        String educationLevel,
        String institutionName,
        String majorFieldOfStudy,
        String currentYearOrSemester,
        BigDecimal cumulativeGpa,
        BigDecimal gpaScale,
        BigDecimal annualFamilyIncome,
        String currencyCode,
        Boolean firstGenStudent,
        Boolean militaryVeteran,
        Boolean disabilityStatus,
        Map<String, Object> additionalAttributes
) {
}
