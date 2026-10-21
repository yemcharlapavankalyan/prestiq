package org.prestiq.student.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.prestiq.student.domain.entity.EducationLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record StudentProfileRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @NotBlank(message = "Citizenship country is required")
        @Pattern(regexp = "^[A-Z]{2}$", message = "Citizenship country must be a valid 2-letter uppercase ISO 3166-1 alpha-2 code (e.g., US, IN)")
        String citizenshipCountry,

        @Size(max = 100, message = "State/province must not exceed 100 characters")
        String stateProvince,

        @NotNull(message = "Education level is required")
        EducationLevel educationLevel,

        @Size(max = 255, message = "Institution name must not exceed 255 characters")
        String institutionName,

        @Size(max = 150, message = "Major/field of study must not exceed 150 characters")
        String majorFieldOfStudy,

        @Size(max = 50, message = "Current year or semester must not exceed 50 characters")
        String currentYearOrSemester,

        @DecimalMin(value = "0.00", message = "Cumulative GPA must be at least 0.00")
        @Digits(integer = 2, fraction = 2, message = "GPA format must have at most 2 integer digits and 2 decimals")
        BigDecimal cumulativeGpa,

        @DecimalMin(value = "0.01", message = "GPA scale must be greater than zero")
        @Digits(integer = 2, fraction = 2, message = "GPA scale format must have at most 2 integer digits and 2 decimals")
        BigDecimal gpaScale,

        @DecimalMin(value = "0.00", message = "Annual family income must be non-negative")
        @Digits(integer = 12, fraction = 2, message = "Income format must have at most 12 integer digits and 2 decimals")
        BigDecimal annualFamilyIncome,

        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be a valid 3-letter uppercase ISO 4217 code (e.g., USD, INR)")
        String currencyCode,

        Boolean firstGenStudent,

        Boolean militaryVeteran,

        Boolean disabilityStatus,

        Map<String, Object> additionalAttributes
) {
}
