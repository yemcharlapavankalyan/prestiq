package org.prestiq.eligibility.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.prestiq.eligibility.domain.entity.CriterionOperator;
import org.prestiq.eligibility.domain.entity.CriterionStatus;
import org.prestiq.eligibility.domain.entity.EligibilityCriterion;
import org.prestiq.eligibility.dto.response.CriterionResultResponse;
import org.prestiq.eligibility.dto.response.StudentProfileDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

@Component
public class NumericRangeEvaluator implements CriterionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(NumericRangeEvaluator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<CriterionOperator> NUMERIC_OPERATORS = Set.of(
            CriterionOperator.GTE,
            CriterionOperator.LTE,
            CriterionOperator.GT,
            CriterionOperator.LT,
            CriterionOperator.EQ,
            CriterionOperator.BETWEEN
    );

    @Override
    public boolean supports(CriterionOperator operator, String targetField) {
        return NUMERIC_OPERATORS.contains(operator);
    }

    @Override
    public CriterionResultResponse evaluate(
            EligibilityCriterion criterion,
            Object studentRawValue,
            StudentProfileDto fullProfile
    ) {
        String field = criterion.getTargetField();
        boolean isGpaField = isGpaField(field);

        // Missing data handling
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
                        "Please update your academic profile with your " + criterion.getDisplayName() + "."
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
                        "Optional criterion '" + criterion.getDisplayName() + "' omitted due to missing profile data.",
                        null
                );
            }
        }

        BigDecimal studentNumber = toBigDecimal(studentRawValue);
        if (studentNumber == null) {
            // Cannot parse as numeric
            if (criterion.isMandatory()) {
                return new CriterionResultResponse(
                        criterion.getCriterionCode(),
                        criterion.getDisplayName(),
                        field,
                        true,
                        CriterionStatus.MISSING_DATA,
                        studentRawValue,
                        criterion.getTargetValue(),
                        "Value for '" + criterion.getDisplayName() + "' is not a valid number.",
                        "Please verify and update your " + criterion.getDisplayName() + " in your profile."
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
                        "Optional criterion '" + criterion.getDisplayName() + "' skipped due to invalid numeric format.",
                        null
                );
            }
        }

        // Parse target specification
        TargetNumericSpec targetSpec = parseTargetSpec(criterion.getTargetValue());

        // Perform GPA scale normalization if applicable
        BigDecimal normalizedStudentNumber = studentNumber;
        if (isGpaField) {
            BigDecimal studentScale = (fullProfile != null && fullProfile.gpaScale() != null && fullProfile.gpaScale().compareTo(BigDecimal.ZERO) > 0)
                    ? fullProfile.gpaScale()
                    : BigDecimal.valueOf(4.00);

            BigDecimal targetScale = targetSpec.scale != null && targetSpec.scale.compareTo(BigDecimal.ZERO) > 0
                    ? targetSpec.scale
                    : BigDecimal.valueOf(4.00);

            if (studentScale.compareTo(targetScale) != 0) {
                // (studentGPA / studentScale) * targetScale
                normalizedStudentNumber = studentNumber
                        .divide(studentScale, 6, RoundingMode.HALF_UP)
                        .multiply(targetScale)
                        .setScale(4, RoundingMode.HALF_UP);
                log.debug("Normalized student GPA {} on scale {} to {} on scale {}",
                        studentNumber, studentScale, normalizedStudentNumber, targetScale);
            }
        }

        // Evaluate according to operator
        boolean passed = evaluateComparison(criterion.getOperator(), normalizedStudentNumber, targetSpec);

        String explanation;
        String remediation = null;

        String studentDisplay = (normalizedStudentNumber.compareTo(studentNumber) != 0)
                ? String.format("%s (normalized from %s)", normalizedStudentNumber.stripTrailingZeros().toPlainString(), studentNumber.stripTrailingZeros().toPlainString())
                : studentNumber.stripTrailingZeros().toPlainString();

        String targetDisplay = targetSpec.getDisplayString();

        if (passed) {
            explanation = formatTemplate(criterion.getPassExplanationTemplate(), studentDisplay, targetDisplay,
                    "Your " + criterion.getDisplayName() + " of " + studentDisplay + " meets the requirement of " + targetDisplay + ".");
        } else {
            explanation = formatTemplate(criterion.getFailExplanationTemplate(), studentDisplay, targetDisplay,
                    "Requires " + criterion.getDisplayName() + " of " + targetDisplay + ", but your profile records " + studentDisplay + ".");
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
                studentNumber,
                targetSpec.getRawTarget(),
                explanation,
                remediation
        );
    }

    private boolean isGpaField(String field) {
        if (field == null) return false;
        String f = field.trim().toLowerCase();
        return f.contains("gpa");
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number num) {
            return BigDecimal.valueOf(num.doubleValue());
        }
        if (value instanceof String str) {
            try {
                return new BigDecimal(str.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private TargetNumericSpec parseTargetSpec(String targetValueStr) {
        TargetNumericSpec spec = new TargetNumericSpec();
        spec.rawTarget = targetValueStr;

        if (targetValueStr == null || targetValueStr.isBlank()) {
            spec.threshold = BigDecimal.ZERO;
            return spec;
        }

        String trimmed = targetValueStr.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                JsonNode json = objectMapper.readTree(trimmed);
                if (json.has("threshold")) {
                    spec.threshold = new BigDecimal(json.get("threshold").asText());
                }
                if (json.has("scale")) {
                    spec.scale = new BigDecimal(json.get("scale").asText());
                }
                if (json.has("min")) {
                    spec.min = new BigDecimal(json.get("min").asText());
                }
                if (json.has("max")) {
                    spec.max = new BigDecimal(json.get("max").asText());
                }
                return spec;
            } catch (Exception e) {
                log.warn("Failed to parse JSON numeric spec: {}", targetValueStr, e);
            }
        }

        if (trimmed.contains(",")) {
            String[] parts = trimmed.split(",");
            if (parts.length >= 2) {
                try {
                    spec.min = new BigDecimal(parts[0].trim());
                    spec.max = new BigDecimal(parts[1].trim());
                    return spec;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        try {
            spec.threshold = new BigDecimal(trimmed);
        } catch (NumberFormatException e) {
            spec.threshold = BigDecimal.ZERO;
        }
        return spec;
    }

    private boolean evaluateComparison(CriterionOperator op, BigDecimal studentVal, TargetNumericSpec spec) {
        return switch (op) {
            case GTE -> {
                BigDecimal threshold = spec.threshold != null ? spec.threshold : spec.min;
                yield threshold != null && studentVal.compareTo(threshold) >= 0;
            }
            case LTE -> {
                BigDecimal threshold = spec.threshold != null ? spec.threshold : spec.max;
                yield threshold != null && studentVal.compareTo(threshold) <= 0;
            }
            case GT -> {
                BigDecimal threshold = spec.threshold != null ? spec.threshold : spec.min;
                yield threshold != null && studentVal.compareTo(threshold) > 0;
            }
            case LT -> {
                BigDecimal threshold = spec.threshold != null ? spec.threshold : spec.max;
                yield threshold != null && studentVal.compareTo(threshold) < 0;
            }
            case EQ -> {
                BigDecimal threshold = spec.threshold;
                yield threshold != null && studentVal.compareTo(threshold) == 0;
            }
            case BETWEEN -> {
                BigDecimal min = spec.min != null ? spec.min : spec.threshold;
                BigDecimal max = spec.max;
                if (min != null && max != null) {
                    yield studentVal.compareTo(min) >= 0 && studentVal.compareTo(max) <= 0;
                }
                yield false;
            }
            default -> false;
        };
    }

    private String formatTemplate(String template, String studentValue, String targetValue, String defaultMsg) {
        if (template != null && !template.isBlank()) {
            return template
                    .replace("{studentValue}", studentValue)
                    .replace("{targetValue}", targetValue);
        }
        return defaultMsg;
    }

    private static class TargetNumericSpec {
        BigDecimal threshold;
        BigDecimal scale;
        BigDecimal min;
        BigDecimal max;
        String rawTarget;

        String getDisplayString() {
            if (min != null && max != null) {
                return "between " + min.stripTrailingZeros().toPlainString() + " and " + max.stripTrailingZeros().toPlainString();
            }
            if (threshold != null) {
                String s = threshold.stripTrailingZeros().toPlainString();
                if (scale != null) {
                    s += " (on a " + scale.stripTrailingZeros().toPlainString() + " scale)";
                }
                return s;
            }
            return rawTarget != null ? rawTarget : "";
        }

        Object getRawTarget() {
            if (min != null && max != null) {
                return min + " - " + max;
            }
            return threshold != null ? threshold : rawTarget;
        }
    }
}
