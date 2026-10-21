package org.prestiq.eligibility.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.prestiq.eligibility.domain.entity.CriterionOperator;
import org.prestiq.eligibility.domain.entity.CriterionStatus;
import org.prestiq.eligibility.domain.entity.EligibilityCriterion;
import org.prestiq.eligibility.dto.response.CriterionResultResponse;
import org.prestiq.eligibility.dto.response.StudentProfileDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SetInclusionEvaluator implements CriterionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(SetInclusionEvaluator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private static final Set<CriterionOperator> SET_OPERATORS = Set.of(
            CriterionOperator.IN,
            CriterionOperator.NOT_IN,
            CriterionOperator.CONTAINS_ANY
    );

    @Override
    public boolean supports(CriterionOperator operator, String targetField) {
        return SET_OPERATORS.contains(operator);
    }

    @Override
    public CriterionResultResponse evaluate(
            EligibilityCriterion criterion,
            Object studentRawValue,
            StudentProfileDto fullProfile
    ) {
        String field = criterion.getTargetField();

        // Check for missing data
        if (studentRawValue == null || isBlankValue(studentRawValue)) {
            if (criterion.isMandatory()) {
                return new CriterionResultResponse(
                        criterion.getCriterionCode(),
                        criterion.getDisplayName(),
                        field,
                        true,
                        CriterionStatus.MISSING_DATA,
                        null,
                        criterion.getTargetValue(),
                        "Required field '" + criterion.getDisplayName() + "' is missing from student profile.",
                        "Please update your profile with your " + criterion.getDisplayName() + "."
                );
            } else {
                return new CriterionResultResponse(
                        criterion.getCriterionCode(),
                        criterion.getDisplayName(),
                        field,
                        false,
                        CriterionStatus.NOT_EVALUATED,
                        null,
                        criterion.getTargetValue(),
                        "Optional criterion '" + criterion.getDisplayName() + "' skipped due to missing profile data.",
                        null
                );
            }
        }

        Set<String> targetSet = parseTargetSet(criterion.getTargetValue());
        Set<String> studentValues = parseStudentValues(studentRawValue);

        boolean passed = switch (criterion.getOperator()) {
            case IN -> studentValues.stream().anyMatch(targetSet::contains);
            case NOT_IN -> studentValues.stream().noneMatch(targetSet::contains);
            case CONTAINS_ANY -> studentValues.stream().anyMatch(targetSet::contains);
            default -> false;
        };

        String studentDisplay = String.join(", ", studentValues);
        String targetDisplay = String.join(", ", targetSet);

        String explanation;
        String remediation = null;

        if (passed) {
            explanation = formatTemplate(criterion.getPassExplanationTemplate(), studentDisplay, targetDisplay,
                    "Your " + criterion.getDisplayName() + " (" + studentDisplay + ") meets the eligibility criteria.");
        } else {
            explanation = formatTemplate(criterion.getFailExplanationTemplate(), studentDisplay, targetDisplay,
                    criterion.getDisplayName() + " requirement not met: " + studentDisplay + " is not in the eligible set (" + targetDisplay + ").");
            if (criterion.isMandatory()) {
                remediation = "Requirement not met: " + explanation;
            }
        }

        return new CriterionResultResponse(
                criterion.getCriterionCode(),
                criterion.getDisplayName(),
                field,
                criterion.isMandatory(),
                passed ? CriterionStatus.PASS : CriterionStatus.FAIL,
                studentDisplay,
                targetSet,
                explanation,
                remediation
        );
    }

    private boolean isBlankValue(Object val) {
        if (val instanceof String s) {
            return s.isBlank();
        }
        if (val instanceof Collection<?> col) {
            return col.isEmpty();
        }
        return false;
    }

    private Set<String> parseStudentValues(Object val) {
        Set<String> set = new HashSet<>();
        if (val instanceof String s) {
            set.add(s.trim().toUpperCase());
        } else if (val instanceof Collection<?> col) {
            for (Object o : col) {
                if (o != null) {
                    set.add(o.toString().trim().toUpperCase());
                }
            }
        } else if (val instanceof Object[] arr) {
            for (Object o : arr) {
                if (o != null) {
                    set.add(o.toString().trim().toUpperCase());
                }
            }
        } else {
            set.add(val.toString().trim().toUpperCase());
        }
        return set;
    }

    private Set<String> parseTargetSet(String targetValue) {
        if (targetValue == null || targetValue.isBlank()) {
            return Set.of();
        }

        String trimmed = targetValue.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                List<String> list = objectMapper.readValue(trimmed, STRING_LIST_TYPE);
                return list.stream()
                        .map(s -> s.trim().toUpperCase())
                        .collect(Collectors.toSet());
            } catch (Exception e) {
                log.warn("Failed to parse JSON target list: {}", targetValue, e);
            }
        }

        // Comma-separated fallback
        return Arrays.stream(trimmed.split(","))
                .map(s -> s.trim().toUpperCase())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private String formatTemplate(String template, String studentValue, String targetValue, String defaultMsg) {
        if (template != null && !template.isBlank()) {
            return template
                    .replace("{studentValue}", studentValue)
                    .replace("{targetValue}", targetValue);
        }
        return defaultMsg;
    }
}
