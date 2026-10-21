package org.prestiq.eligibility.engine;

import org.prestiq.eligibility.domain.entity.CriterionStatus;
import org.prestiq.eligibility.domain.entity.EligibilityCriterion;
import org.prestiq.eligibility.domain.entity.ScholarshipRuleSet;
import org.prestiq.eligibility.domain.entity.Verdict;
import org.prestiq.eligibility.dto.response.CriterionResultResponse;
import org.prestiq.eligibility.dto.response.StudentProfileDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class DeterministicEligibilityEngine {

    private static final Logger log = LoggerFactory.getLogger(DeterministicEligibilityEngine.class);

    private final StudentFieldResolver fieldResolver;
    private final EvaluatorRegistry evaluatorRegistry;

    public DeterministicEligibilityEngine(StudentFieldResolver fieldResolver, EvaluatorRegistry evaluatorRegistry) {
        this.fieldResolver = fieldResolver;
        this.evaluatorRegistry = evaluatorRegistry;
    }

    public EngineEvaluationResult evaluate(ScholarshipRuleSet ruleSet, StudentProfileDto studentProfile) {
        if (ruleSet == null) {
            throw new IllegalArgumentException("Scholarship rule set cannot be null");
        }

        List<EligibilityCriterion> criteria = ruleSet.getCriteria();
        if (criteria == null || criteria.isEmpty()) {
            // Empty criteria rule set: automatically eligible with 100% match
            return new EngineEvaluationResult(
                    Verdict.ELIGIBLE,
                    BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP),
                    List.of(),
                    0,
                    0,
                    false
            );
        }

        List<CriterionResultResponse> breakdown = new ArrayList<>();
        int mandatoryCount = 0;
        int mandatoryPassCount = 0;
        int mandatoryFailCount = 0;
        int mandatoryMissingCount = 0;

        int preferredCount = 0;
        int preferredPassWeight = 0;
        int totalPreferredWeight = 0;

        for (EligibilityCriterion criterion : criteria) {
            Object rawStudentValue = fieldResolver.resolveField(studentProfile, criterion.getTargetField());
            CriterionEvaluator evaluator = evaluatorRegistry.findEvaluator(criterion);
            CriterionResultResponse result = evaluator.evaluate(criterion, rawStudentValue, studentProfile);
            breakdown.add(result);

            if (criterion.isMandatory()) {
                mandatoryCount++;
                if (result.status() == CriterionStatus.PASS) {
                    mandatoryPassCount++;
                } else if (result.status() == CriterionStatus.FAIL) {
                    mandatoryFailCount++;
                } else if (result.status() == CriterionStatus.MISSING_DATA) {
                    mandatoryMissingCount++;
                }
            } else {
                preferredCount++;
                int weight = Math.max(criterion.getWeight(), 1);
                totalPreferredWeight += weight;
                if (result.status() == CriterionStatus.PASS) {
                    preferredPassWeight += weight;
                }
            }
        }

        // Determine Verdict with strict precedence:
        // 1. INELIGIBLE: any mandatory criterion failed (takes absolute precedence over missing data)
        // 2. NEEDS_VERIFICATION: no mandatory failed, but at least one mandatory is missing
        // 3. ELIGIBLE: all mandatory criteria passed
        Verdict verdict;
        if (mandatoryFailCount > 0) {
            verdict = Verdict.INELIGIBLE;
        } else if (mandatoryMissingCount > 0) {
            verdict = Verdict.NEEDS_VERIFICATION;
        } else {
            verdict = Verdict.ELIGIBLE;
        }

        // Calculate Match Percentage
        BigDecimal matchPercentage = calculateMatchPercentage(
                verdict,
                mandatoryCount,
                mandatoryPassCount,
                preferredCount,
                preferredPassWeight,
                totalPreferredWeight
        );

        boolean hasMissingData = (mandatoryMissingCount > 0);
        int totalPassed = mandatoryPassCount + (preferredCount > 0 ? (preferredPassWeight > 0 ? 1 : 0) : 0);

        return new EngineEvaluationResult(
                verdict,
                matchPercentage,
                breakdown,
                mandatoryPassCount,
                criteria.size(),
                hasMissingData
        );
    }

    private BigDecimal calculateMatchPercentage(
            Verdict verdict,
            int mandatoryCount,
            int mandatoryPassCount,
            int preferredCount,
            int preferredPassWeight,
            int totalPreferredWeight
    ) {
        if (verdict == Verdict.INELIGIBLE) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (verdict == Verdict.NEEDS_VERIFICATION) {
            if (mandatoryCount == 0) {
                return BigDecimal.valueOf(50.00).setScale(2, RoundingMode.HALF_UP);
            }
            double passRatio = (double) mandatoryPassCount / mandatoryCount;
            return BigDecimal.valueOf(passRatio * 100.0).setScale(2, RoundingMode.HALF_UP);
        }

        // ELIGIBLE verdict
        if (preferredCount == 0 || totalPreferredWeight == 0) {
            return BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP);
        }

        // Baseline 80% for 100% mandatory pass + up to 20% for preferred criteria match
        double preferredRatio = (double) preferredPassWeight / totalPreferredWeight;
        double finalScore = 80.0 + (20.0 * preferredRatio);
        return BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP);
    }

    public record EngineEvaluationResult(
            Verdict verdict,
            BigDecimal matchPercentage,
            List<CriterionResultResponse> breakdown,
            int passedCriteriaCount,
            int totalCriteriaCount,
            boolean hasMissingData
    ) {
    }
}
