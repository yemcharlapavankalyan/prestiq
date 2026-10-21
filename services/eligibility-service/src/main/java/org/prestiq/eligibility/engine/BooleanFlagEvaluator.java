package org.prestiq.eligibility.engine;

import org.prestiq.eligibility.domain.entity.CriterionOperator;
import org.prestiq.eligibility.domain.entity.CriterionStatus;
import org.prestiq.eligibility.domain.entity.EligibilityCriterion;
import org.prestiq.eligibility.dto.response.CriterionResultResponse;
import org.prestiq.eligibility.dto.response.StudentProfileDto;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class BooleanFlagEvaluator implements CriterionEvaluator {

    private static final Set<CriterionOperator> BOOLEAN_OPERATORS = Set.of(
            CriterionOperator.IS_TRUE,
            CriterionOperator.IS_FALSE
    );

    @Override
    public boolean supports(CriterionOperator operator, String targetField) {
        if (BOOLEAN_OPERATORS.contains(operator)) {
            return true;
        }
        // EQ is also supported for boolean fields
        return operator == CriterionOperator.EQ && isBooleanField(targetField);
    }

    private boolean isBooleanField(String targetField) {
        if (targetField == null) return false;
        String f = targetField.toLowerCase();
        return f.contains("firstgen") || f.contains("veteran") || f.contains("disability") || f.startsWith("is");
    }

    @Override
    public CriterionResultResponse evaluate(
            EligibilityCriterion criterion,
            Object studentRawValue,
            StudentProfileDto fullProfile
    ) {
        String field = criterion.getTargetField();

        if (studentRawValue == null) {
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
                        "Please update your profile with your " + criterion.getDisplayName() + " status."
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
                        "Optional criterion '" + criterion.getDisplayName() + "' skipped because it is not specified.",
                        null
                );
            }
        }

        Boolean studentBool = toBoolean(studentRawValue);
        if (studentBool == null) {
            if (criterion.isMandatory()) {
                return new CriterionResultResponse(
                        criterion.getCriterionCode(),
                        criterion.getDisplayName(),
                        field,
                        true,
                        CriterionStatus.MISSING_DATA,
                        studentRawValue,
                        criterion.getTargetValue(),
                        "Value for '" + criterion.getDisplayName() + "' is not a valid boolean.",
                        "Please update your profile with a valid yes/no for " + criterion.getDisplayName() + "."
                );
            } else {
                return new CriterionResultResponse(
                        criterion.getCriterionCode(),
                        criterion.getDisplayName(),
                        field,
                        false,
                        CriterionStatus.NOT_EVALUATED,
                        studentRawValue,
                        criterion.getTargetValue(),
                        "Optional criterion '" + criterion.getDisplayName() + "' skipped due to invalid boolean format.",
                        null
                );
            }
        }

        boolean passed;
        if (criterion.getOperator() == CriterionOperator.IS_TRUE) {
            passed = studentBool;
        } else if (criterion.getOperator() == CriterionOperator.IS_FALSE) {
            passed = !studentBool;
        } else {
            // EQ operator
            boolean targetBool = parseTargetBoolean(criterion.getTargetValue());
            passed = (studentBool == targetBool);
        }

        String studentDisplay = studentBool ? "Yes" : "No";
        String targetDisplay = (criterion.getOperator() == CriterionOperator.IS_TRUE || parseTargetBoolean(criterion.getTargetValue())) ? "Yes" : "No";

        String explanation;
        String remediation = null;

        if (passed) {
            explanation = formatTemplate(criterion.getPassExplanationTemplate(), studentDisplay, targetDisplay,
                    "Your status for " + criterion.getDisplayName() + " satisfies the eligibility criterion.");
        } else {
            explanation = formatTemplate(criterion.getFailExplanationTemplate(), studentDisplay, targetDisplay,
                    "Requirement for " + criterion.getDisplayName() + " was not met.");
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
                studentBool,
                criterion.getTargetValue(),
                explanation,
                remediation
        );
    }

    private Boolean toBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            String trimmed = s.trim().toLowerCase();
            if (trimmed.equals("true") || trimmed.equals("yes") || trimmed.equals("1")) {
                return true;
            }
            if (trimmed.equals("false") || trimmed.equals("no") || trimmed.equals("0")) {
                return false;
            }
        }
        return null;
    }

    private boolean parseTargetBoolean(String targetValue) {
        if (targetValue == null) return true;
        String t = targetValue.trim().toLowerCase();
        return t.equals("true") || t.equals("yes") || t.equals("1");
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
