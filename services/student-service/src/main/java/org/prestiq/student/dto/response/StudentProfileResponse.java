package org.prestiq.student.dto.response;

import org.prestiq.student.domain.entity.EducationLevel;
import org.prestiq.student.domain.entity.StudentProfile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record StudentProfileResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String fullName,
        LocalDate dateOfBirth,
        String citizenshipCountry,
        String stateProvince,
        EducationLevel educationLevel,
        String institutionName,
        String majorFieldOfStudy,
        String currentYearOrSemester,
        BigDecimal cumulativeGpa,
        BigDecimal gpaScale,
        BigDecimal annualFamilyIncome,
        String currencyCode,
        boolean firstGenStudent,
        boolean militaryVeteran,
        boolean disabilityStatus,
        Map<String, Object> additionalAttributes,
        Instant createdAt,
        Instant updatedAt
) {
    public static StudentProfileResponse fromEntity(StudentProfile profile) {
        if (profile == null) {
            return null;
        }
        return new StudentProfileResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getFullName(),
                profile.getDateOfBirth(),
                profile.getCitizenshipCountry(),
                profile.getStateProvince(),
                profile.getEducationLevel(),
                profile.getInstitutionName(),
                profile.getMajorFieldOfStudy(),
                profile.getCurrentYearOrSemester(),
                profile.getCumulativeGpa(),
                profile.getGpaScale(),
                profile.getAnnualFamilyIncome(),
                profile.getCurrencyCode(),
                profile.isFirstGenStudent(),
                profile.isMilitaryVeteran(),
                profile.isDisabilityStatus(),
                profile.getAdditionalAttributes(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
