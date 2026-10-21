package org.prestiq.eligibility.engine;

import org.prestiq.eligibility.domain.entity.CriterionOperator;
import org.prestiq.eligibility.domain.entity.CriterionStatus;
import org.prestiq.eligibility.domain.entity.EligibilityCriterion;
import org.prestiq.eligibility.dto.response.CriterionResultResponse;
import org.prestiq.eligibility.dto.response.StudentProfileDto;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class StringMatchEvaluator implements CriterionEvaluator {

    private static final Set<CriterionOperator> STRING_OPERATORS = Set.of(
            CriterionOperator.EQUALS,
            CriterionOperator.NOT_EQUALS
    );

    @Override
    public boolean supports(CriterionOperator operator, String targetField) {
        if (STRING_OPERATORS.contains(operator)) {
            return true;
        }
        // Fallback for EQ on general text fields
        return operator == CriterionOperator.EQ && isStringField(targetField);
    }

    private boolean isStringField(String targetField) {
        if (targetField == null) return false;
        String f = targetField.toLowerCase();
        return f.contains("name") || f.contains("major") || f.contains("institution") || f.contains("semester") || f.contains("year");
    }

    @Override
    public CriterionResultResponse evaluate(
            EligibilityCriterion criterion,
            Object studentRawValue,
            StudentProfileDto fullProfile
    ) {
        String field = criterion.getTargetField();

        if (studentRawValue == null || (studentRawValue instanceof String s && s.isBlank())) {
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

        String studentStr = studentRawValue.toString().trim();
        String targetStr = criterion.getTargetValue() != null ? criterion.getTargetValue().trim() : "";

        boolean match = studentStr.equalsIgnoreCase(targetStr);
        boolean passed = (criterion.getOperator() == CriterionOperator.NOT_EQUALS) ? !match : match;

        String explanation;
        String remediation = null;

        if (passed) {
            explanation = formatTemplate(criterion.getPassExplanationTemplate(), studentStr, targetStr,
                    "Your " + criterion.getDisplayName() + " (" + studentStr + ") matches the requirement.");
        } else {
            explanation = formatTemplate(criterion.getFailExplanationTemplate(), studentStr, targetStr,
                    "Requires " + criterion.getDisplayName() + " of '" + targetStr + "', but found '" + studentStr + "'.");
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
                studentStr,
                targetStr,
                explanation,
                remediation
        );
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
