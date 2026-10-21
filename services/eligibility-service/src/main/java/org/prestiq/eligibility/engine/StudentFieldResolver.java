package org.prestiq.eligibility.engine;

import org.prestiq.eligibility.dto.response.StudentProfileDto;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class StudentFieldResolver {

    private static final Set<String> ALLOWLISTED_FIELDS = Set.of(
            "academic.cumulativeGpa",
            "academic.gpaScale",
            "academic.educationLevel",
            "academic.institutionName",
            "academic.majorFieldOfStudy",
            "academic.currentYearOrSemester",
            "financial.annualFamilyIncome",
            "financial.currencyCode",
            "demographics.citizenshipCountry",
            "demographics.stateProvince",
            "demographics.firstGenStudent",
            "demographics.militaryVeteran",
            "demographics.disabilityStatus",
            // Flat aliases
            "cumulativeGpa",
            "gpaScale",
            "educationLevel",
            "institutionName",
            "majorFieldOfStudy",
            "currentYearOrSemester",
            "annualFamilyIncome",
            "currencyCode",
            "citizenshipCountry",
            "stateProvince",
            "firstGenStudent",
            "militaryVeteran",
            "disabilityStatus"
    );

    public boolean isAllowedField(String targetField) {
        if (targetField == null || targetField.isBlank()) {
            return false;
        }
        String field = targetField.trim();
        if (ALLOWLISTED_FIELDS.contains(field)) {
            return true;
        }
        return field.startsWith("additionalAttributes.") || field.startsWith("demographics.additionalAttributes.");
    }

    public Object resolveField(StudentProfileDto student, String targetField) {
        if (student == null || targetField == null) {
            return null;
        }

        String field = targetField.trim();

        return switch (field) {
            case "academic.cumulativeGpa", "cumulativeGpa" -> student.cumulativeGpa();
            case "academic.gpaScale", "gpaScale" -> student.gpaScale();
            case "academic.educationLevel", "educationLevel" -> student.educationLevel();
            case "academic.institutionName", "institutionName" -> student.institutionName();
            case "academic.majorFieldOfStudy", "majorFieldOfStudy" -> student.majorFieldOfStudy();
            case "academic.currentYearOrSemester", "currentYearOrSemester" -> student.currentYearOrSemester();
            case "financial.annualFamilyIncome", "annualFamilyIncome" -> student.annualFamilyIncome();
            case "financial.currencyCode", "currencyCode" -> student.currencyCode();
            case "demographics.citizenshipCountry", "citizenshipCountry" -> student.citizenshipCountry();
            case "demographics.stateProvince", "stateProvince" -> student.stateProvince();
            case "demographics.firstGenStudent", "firstGenStudent" -> student.firstGenStudent();
            case "demographics.militaryVeteran", "militaryVeteran" -> student.militaryVeteran();
            case "demographics.disabilityStatus", "disabilityStatus" -> student.disabilityStatus();
            default -> {
                if (field.startsWith("additionalAttributes.") || field.startsWith("demographics.additionalAttributes.")) {
                    String key = field.substring(field.lastIndexOf('.') + 1);
                    Map<String, Object> map = student.additionalAttributes();
                    yield (map != null) ? map.get(key) : null;
                }
                yield null;
            }
        };
    }
}
