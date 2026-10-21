package org.prestiq.recommendation.engine;

import org.prestiq.recommendation.domain.entity.EligibilityVerdict;
import org.prestiq.recommendation.domain.entity.StudentPreference;
import org.prestiq.recommendation.dto.client.BatchEvaluateItemDto;
import org.prestiq.recommendation.dto.client.ScholarshipSummaryClientDto;
import org.prestiq.recommendation.dto.client.StudentProfileClientDto;
import org.prestiq.recommendation.dto.response.ScoreBreakdownDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class RecommendationRankingEngine {

    public RankedResult scoreScholarship(
            ScholarshipSummaryClientDto scholarship,
            BatchEvaluateItemDto evaluation,
            StudentProfileClientDto student,
            StudentPreference preferences,
            LocalDate currentDate
    ) {
        LocalDate now = (currentDate != null) ? currentDate : LocalDate.now();

        // 1. Strict Eligibility Gate:
        // If evaluation is missing, or verdict is INELIGIBLE, ranking NEVER overrides ineligibility!
        if (evaluation == null || "INELIGIBLE".equalsIgnoreCase(evaluation.verdict())) {
            ScoreBreakdownDto breakdown = new ScoreBreakdownDto(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    RankingFactors.WEIGHTS_MAP,
                    "Ineligible based on deterministic eligibility engine criteria."
            );
            return new RankedResult(
                    scholarship.id(),
                    false,
                    null,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    breakdown,
                    0
            );
        }

        EligibilityVerdict verdict = "NEEDS_VERIFICATION".equalsIgnoreCase(evaluation.verdict())
                ? EligibilityVerdict.NEEDS_VERIFICATION
                : EligibilityVerdict.ELIGIBLE;

        // 2. Factor Computations
        BigDecimal eligibilityScore = RankingFactors.calculateEligibilityScore(evaluation);
        BigDecimal academicScore = RankingFactors.calculateAcademicAffinityScore(student, scholarship, preferences);
        BigDecimal urgencyScore = RankingFactors.calculateDeadlineUrgencyScore(scholarship.applicationDeadline(), now);
        BigDecimal minPref = (preferences != null) ? preferences.getMinAwardPreference() : null;
        BigDecimal awardScore = RankingFactors.calculateAwardFactorScore(scholarship.awardAmountMax(), minPref);

        // 3. Composite Weighted Score
        double composite = (eligibilityScore.doubleValue() * RankingFactors.WEIGHT_ELIGIBILITY)
                + (academicScore.doubleValue() * RankingFactors.WEIGHT_ACADEMIC)
                + (urgencyScore.doubleValue() * RankingFactors.WEIGHT_URGENCY)
                + (awardScore.doubleValue() * RankingFactors.WEIGHT_AWARD);

        BigDecimal finalMatchScore = BigDecimal.valueOf(composite).setScale(2, RoundingMode.HALF_UP);

        // 4. Human-readable explainable summary
        long daysUntil = (scholarship.applicationDeadline() != null)
                ? ChronoUnit.DAYS.between(now, scholarship.applicationDeadline())
                : -1;

        String explanation = buildExplanation(verdict, eligibilityScore, academicScore, daysUntil, scholarship.awardAmountMax());

        ScoreBreakdownDto breakdown = new ScoreBreakdownDto(
                eligibilityScore,
                academicScore,
                urgencyScore,
                awardScore,
                RankingFactors.WEIGHTS_MAP,
                explanation
        );

        return new RankedResult(
                scholarship.id(),
                true,
                verdict,
                eligibilityScore,
                finalMatchScore,
                breakdown,
                evaluation.ruleSetVersion()
        );
    }

    private String buildExplanation(
            EligibilityVerdict verdict,
            BigDecimal eligibilityScore,
            BigDecimal academicScore,
            long daysUntil,
            BigDecimal awardMax
    ) {
        StringBuilder sb = new StringBuilder();
        if (verdict == EligibilityVerdict.ELIGIBLE) {
            sb.append("Deterministic eligibility verified (").append(eligibilityScore).append("%). ");
        } else {
            sb.append("Requires profile verification for some criteria. ");
        }

        if (academicScore.compareTo(BigDecimal.valueOf(70)) >= 0) {
            sb.append("Strong academic alignment. ");
        }

        if (daysUntil >= 0 && daysUntil <= 30) {
            sb.append("Deadline approaching in ").append(daysUntil).append(" days! ");
        }

        if (awardMax != null && awardMax.compareTo(BigDecimal.valueOf(5000)) >= 0) {
            sb.append("High-value award opportunity ($").append(awardMax.stripTrailingZeros().toPlainString()).append(").");
        }

        return sb.toString().trim();
    }

    public record RankedResult(
            Long scholarshipId,
            boolean isEligible,
            EligibilityVerdict verdict,
            BigDecimal eligibilityScore,
            BigDecimal matchScore,
            ScoreBreakdownDto breakdown,
            int ruleSetVersion
    ) {
    }
}
