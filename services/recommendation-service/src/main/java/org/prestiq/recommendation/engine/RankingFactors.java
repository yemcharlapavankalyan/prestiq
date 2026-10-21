package org.prestiq.recommendation.engine;

import org.prestiq.recommendation.domain.entity.StudentPreference;
import org.prestiq.recommendation.dto.client.BatchEvaluateItemDto;
import org.prestiq.recommendation.dto.client.ScholarshipSummaryClientDto;
import org.prestiq.recommendation.dto.client.StudentProfileClientDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class RankingFactors {

    public static final double WEIGHT_ELIGIBILITY = 0.50;
    public static final double WEIGHT_ACADEMIC = 0.20;
    public static final double WEIGHT_URGENCY = 0.15;
    public static final double WEIGHT_AWARD = 0.15;

    public static final Map<String, Double> WEIGHTS_MAP = Map.of(
            "eligibility", WEIGHT_ELIGIBILITY,
            "academicAffinity", WEIGHT_ACADEMIC,
            "deadlineUrgency", WEIGHT_URGENCY,
            "awardFactor", WEIGHT_AWARD
    );

    public static BigDecimal calculateEligibilityScore(BatchEvaluateItemDto eval) {
        if (eval == null || "INELIGIBLE".equalsIgnoreCase(eval.verdict())) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (eval.matchPercentage() != null) {
            return eval.matchPercentage().setScale(2, RoundingMode.HALF_UP);
        }
        return "ELIGIBLE".equalsIgnoreCase(eval.verdict())
                ? BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(50.00).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateAcademicAffinityScore(
            StudentProfileClientDto student,
            ScholarshipSummaryClientDto scholarship,
            StudentPreference preferences
    ) {
        double score = 50.0;

        String studentMajor = (student != null && student.majorFieldOfStudy() != null)
                ? student.majorFieldOfStudy().trim().toLowerCase()
                : "";

        String studentEdLevel = (student != null && student.educationLevel() != null)
                ? student.educationLevel().trim().toLowerCase()
                : "";

        // Check education level alignment
        if (!studentEdLevel.isEmpty() && scholarship.educationLevels() != null && !scholarship.educationLevels().isEmpty()) {
            boolean edMatch = scholarship.educationLevels().stream()
                    .anyMatch(lvl -> lvl.toLowerCase().contains(studentEdLevel) || studentEdLevel.contains(lvl.toLowerCase()));
            if (edMatch) {
                score += 20.0;
            }
        } else if (scholarship.educationLevels() == null || scholarship.educationLevels().isEmpty()) {
            score += 15.0; // Open to any level
        }

        // Check preferred fields or student major alignment
        List<String> preferredFields = (preferences != null && preferences.getPreferredFields() != null)
                ? preferences.getPreferredFields()
                : List.of();

        if (!studentMajor.isEmpty()) {
            String titleAndProvider = (scholarship.title() + " " + scholarship.providerName()).toLowerCase();
            if (titleAndProvider.contains(studentMajor)) {
                score += 30.0;
            } else if (!preferredFields.isEmpty()) {
                boolean prefMatch = preferredFields.stream()
                        .anyMatch(f -> titleAndProvider.contains(f.toLowerCase()));
                if (prefMatch) {
                    score += 25.0;
                } else {
                    score += 10.0;
                }
            } else {
                score += 15.0;
            }
        } else {
            score += 10.0;
        }

        double finalScore = Math.min(100.0, Math.max(0.0, score));
        return BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateDeadlineUrgencyScore(LocalDate deadline, LocalDate currentDate) {
        if (deadline == null) {
            return BigDecimal.valueOf(50.00).setScale(2, RoundingMode.HALF_UP);
        }

        long daysUntil = ChronoUnit.DAYS.between(currentDate != null ? currentDate : LocalDate.now(), deadline);

        double score;
        if (daysUntil < 0) {
            score = 0.0; // Expired
        } else if (daysUntil <= 14) {
            score = 100.0; // Urgent: within 2 weeks
        } else if (daysUntil <= 30) {
            score = 85.0; // Approaching: within 1 month
        } else if (daysUntil <= 60) {
            score = 70.0; // Upcoming: within 2 months
        } else if (daysUntil <= 90) {
            score = 50.0; // Within 3 months
        } else {
            score = 30.0; // Distant deadline
        }

        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateAwardFactorScore(
            BigDecimal awardMax,
            BigDecimal minAwardPreference
    ) {
        if (awardMax == null || awardMax.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.valueOf(40.00).setScale(2, RoundingMode.HALF_UP);
        }

        double score;
        if (minAwardPreference != null && minAwardPreference.compareTo(BigDecimal.ZERO) > 0) {
            if (awardMax.compareTo(minAwardPreference) >= 0) {
                score = 100.0;
            } else {
                score = awardMax.divide(minAwardPreference, 4, RoundingMode.HALF_UP).doubleValue() * 80.0;
            }
        } else {
            // General scaling benchmark: $10,000+ = 100
            double amount = awardMax.doubleValue();
            if (amount >= 10000.0) {
                score = 100.0;
            } else if (amount >= 5000.0) {
                score = 80.0 + ((amount - 5000.0) / 5000.0 * 20.0);
            } else if (amount >= 2000.0) {
                score = 60.0 + ((amount - 2000.0) / 3000.0 * 20.0);
            } else if (amount >= 500.0) {
                score = 40.0 + ((amount - 500.0) / 1500.0 * 20.0);
            } else {
                score = 30.0;
            }
        }

        double finalScore = Math.min(100.0, Math.max(0.0, score));
        return BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP);
    }
}
