package org.prestiq.recommendation.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.prestiq.recommendation.domain.entity.EligibilityVerdict;
import org.prestiq.recommendation.domain.entity.StudentPreference;
import org.prestiq.recommendation.dto.client.BatchEvaluateItemDto;
import org.prestiq.recommendation.dto.client.ScholarshipSummaryClientDto;
import org.prestiq.recommendation.dto.client.StudentProfileClientDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Recommendation Ranking Engine & Factors Unit Tests")
class RecommendationRankingEngineTest {

    private RecommendationRankingEngine rankingEngine;
    private LocalDate fixedToday;

    @BeforeEach
    void setUp() {
        rankingEngine = new RecommendationRankingEngine();
        fixedToday = LocalDate.of(2026, 10, 21);
    }

    @Test
    @DisplayName("Weights configuration sums precisely to 1.00")
    void testWeightsSumToUnity() {
        double total = RankingFactors.WEIGHT_ELIGIBILITY
                + RankingFactors.WEIGHT_ACADEMIC
                + RankingFactors.WEIGHT_URGENCY
                + RankingFactors.WEIGHT_AWARD;

        assertEquals(1.00, total, 0.0001, "Configured weights must sum exactly to 1.00");
        assertEquals(0.50, RankingFactors.WEIGHTS_MAP.get("eligibility"));
        assertEquals(0.20, RankingFactors.WEIGHTS_MAP.get("academicAffinity"));
        assertEquals(0.15, RankingFactors.WEIGHTS_MAP.get("deadlineUrgency"));
        assertEquals(0.15, RankingFactors.WEIGHTS_MAP.get("awardFactor"));
    }

    @Test
    @DisplayName("Eligibility factor scoring accurately maps verdicts and match percentages")
    void testCalculateEligibilityScore() {
        // Ineligible or null always yields 0.00
        assertEquals(new BigDecimal("0.00"), RankingFactors.calculateEligibilityScore(null));
        BatchEvaluateItemDto ineligible = new BatchEvaluateItemDto(1L, 1, "INELIGIBLE", new BigDecimal("40.00"), 2, 5, false);
        assertEquals(new BigDecimal("0.00"), RankingFactors.calculateEligibilityScore(ineligible));

        // Eligible with explicit match percentage
        BatchEvaluateItemDto eligibleWithPct = new BatchEvaluateItemDto(1L, 1, "ELIGIBLE", new BigDecimal("95.00"), 5, 5, false);
        assertEquals(new BigDecimal("95.00"), RankingFactors.calculateEligibilityScore(eligibleWithPct));

        // Eligible without match percentage defaults to 100.00
        BatchEvaluateItemDto eligibleNoPct = new BatchEvaluateItemDto(1L, 1, "ELIGIBLE", null, 5, 5, false);
        assertEquals(new BigDecimal("100.00"), RankingFactors.calculateEligibilityScore(eligibleNoPct));

        // Needs verification with explicit match percentage
        BatchEvaluateItemDto needsVerWithPct = new BatchEvaluateItemDto(1L, 1, "NEEDS_VERIFICATION", new BigDecimal("75.00"), 3, 5, true);
        assertEquals(new BigDecimal("75.00"), RankingFactors.calculateEligibilityScore(needsVerWithPct));

        // Needs verification without match percentage defaults to 50.00
        BatchEvaluateItemDto needsVerNoPct = new BatchEvaluateItemDto(1L, 1, "NEEDS_VERIFICATION", null, 3, 5, true);
        assertEquals(new BigDecimal("50.00"), RankingFactors.calculateEligibilityScore(needsVerNoPct));
    }

    @Test
    @DisplayName("Academic affinity scoring incorporates major, education level, and preferred fields")
    void testCalculateAcademicAffinityScore() {
        StudentProfileClientDto student = new StudentProfileClientDto(
                UUID.randomUUID(), UUID.randomUUID(), "Jane", "Doe", "Jane Doe",
                LocalDate.of(2003, 5, 12), "US", "CA", "UNDERGRADUATE",
                "Stanford University", "Computer Science", "Junior",
                new BigDecimal("3.85"), new BigDecimal("4.00"), new BigDecimal("55000"),
                "USD", false, false, false, null
        );

        ScholarshipSummaryClientDto scholarshipMatchingMajorAndEd = new ScholarshipSummaryClientDto(
                101L, "cs-excellence", "Computer Science Excellence Scholarship", "Tech Foundation",
                "FOUNDATION", new BigDecimal("5000"), new BigDecimal("5000"), "USD",
                "ONE_TIME", false, fixedToday.minusDays(10), fixedToday.plusDays(30), false,
                List.of("US"), List.of("UNDERGRADUATE"), new BigDecimal("3.50"), "VERIFIED", "PUBLISHED"
        );

        // Matches major in title (+30) and education level (+20) -> 50 base + 30 + 20 = 100.00
        BigDecimal score = RankingFactors.calculateAcademicAffinityScore(student, scholarshipMatchingMajorAndEd, null);
        assertEquals(new BigDecimal("100.00"), score);

        // Student with different major but preference matches
        StudentProfileClientDto studentBio = new StudentProfileClientDto(
                UUID.randomUUID(), UUID.randomUUID(), "Bob", "Smith", "Bob Smith",
                LocalDate.of(2002, 1, 1), "US", "NY", "POSTGRADUATE",
                "NYU", "Biology", "1st Year",
                new BigDecimal("3.70"), new BigDecimal("4.00"), null,
                "USD", false, false, false, null
        );

        StudentPreference prefWithTech = new StudentPreference(
                UUID.randomUUID(), studentBio.userId(), List.of("Tech"), List.of(), null, Instant.now()
        );

        BigDecimal scorePrefMatch = RankingFactors.calculateAcademicAffinityScore(studentBio, scholarshipMatchingMajorAndEd, prefWithTech);
        // Base 50, education level doesn't match (UNDERGRADUATE vs POSTGRADUATE), pref matches "Tech" in providerName (+25) -> 75.00
        assertEquals(new BigDecimal("75.00"), scorePrefMatch);
    }

    @Test
    @DisplayName("Deadline urgency scoring prioritizes imminent deadlines and handles expired/null")
    void testCalculateDeadlineUrgencyScore() {
        // Expired (yesterday)
        assertEquals(new BigDecimal("0.00"), RankingFactors.calculateDeadlineUrgencyScore(fixedToday.minusDays(1), fixedToday));

        // Urgent (within 14 days)
        assertEquals(new BigDecimal("100.00"), RankingFactors.calculateDeadlineUrgencyScore(fixedToday.plusDays(7), fixedToday));
        assertEquals(new BigDecimal("100.00"), RankingFactors.calculateDeadlineUrgencyScore(fixedToday.plusDays(14), fixedToday));

        // Approaching (15 to 30 days)
        assertEquals(new BigDecimal("85.00"), RankingFactors.calculateDeadlineUrgencyScore(fixedToday.plusDays(25), fixedToday));

        // Upcoming (31 to 60 days)
        assertEquals(new BigDecimal("70.00"), RankingFactors.calculateDeadlineUrgencyScore(fixedToday.plusDays(45), fixedToday));

        // Within 90 days
        assertEquals(new BigDecimal("50.00"), RankingFactors.calculateDeadlineUrgencyScore(fixedToday.plusDays(80), fixedToday));

        // Distant (> 90 days)
        assertEquals(new BigDecimal("30.00"), RankingFactors.calculateDeadlineUrgencyScore(fixedToday.plusDays(120), fixedToday));

        // Null deadline
        assertEquals(new BigDecimal("50.00"), RankingFactors.calculateDeadlineUrgencyScore(null, fixedToday));
    }

    @Test
    @DisplayName("Award factor scoring scales against preference or benchmark tiers")
    void testCalculateAwardFactorScore() {
        // Null or 0 award
        assertEquals(new BigDecimal("40.00"), RankingFactors.calculateAwardFactorScore(null, null));
        assertEquals(new BigDecimal("40.00"), RankingFactors.calculateAwardFactorScore(BigDecimal.ZERO, null));

        // With minimum award preference: $5000 pref, $10000 award -> 100.00
        assertEquals(new BigDecimal("100.00"), RankingFactors.calculateAwardFactorScore(new BigDecimal("10000"), new BigDecimal("5000")));

        // With minimum award preference: $10000 pref, $5000 award -> 5000/10000 * 80 = 40.00
        assertEquals(new BigDecimal("40.00"), RankingFactors.calculateAwardFactorScore(new BigDecimal("5000"), new BigDecimal("10000")));

        // Without preference: Tiered benchmarks
        assertEquals(new BigDecimal("100.00"), RankingFactors.calculateAwardFactorScore(new BigDecimal("15000"), null));
        assertEquals(new BigDecimal("80.00"), RankingFactors.calculateAwardFactorScore(new BigDecimal("5000"), null));
        assertEquals(new BigDecimal("60.00"), RankingFactors.calculateAwardFactorScore(new BigDecimal("2000"), null));
        assertEquals(new BigDecimal("40.00"), RankingFactors.calculateAwardFactorScore(new BigDecimal("500"), null));
        assertEquals(new BigDecimal("30.00"), RankingFactors.calculateAwardFactorScore(new BigDecimal("250"), null));
    }

    @Test
    @DisplayName("Strict Eligibility Gate: INELIGIBLE or missing evaluation is never recommended")
    void testStrictEligibilityGate() {
        ScholarshipSummaryClientDto scholarship = new ScholarshipSummaryClientDto(
                201L, "math-schol", "Mathematics Scholar", "Math Org", "GOVERNMENT",
                new BigDecimal("20000"), new BigDecimal("20000"), "USD", "ONE_TIME",
                false, fixedToday.minusDays(5), fixedToday.plusDays(10), false,
                List.of("US"), List.of("UNDERGRADUATE"), new BigDecimal("3.00"),
                "VERIFIED", "PUBLISHED"
        );

        StudentProfileClientDto student = new StudentProfileClientDto(
                UUID.randomUUID(), UUID.randomUUID(), "Alice", "Wonder", "Alice Wonder",
                LocalDate.of(2004, 3, 1), "US", "NY", "UNDERGRADUATE",
                "NYU", "Mathematics", "Sophomore",
                new BigDecimal("3.90"), new BigDecimal("4.00"), null,
                "USD", false, false, false, null
        );

        // 1. Missing evaluation
        RecommendationRankingEngine.RankedResult missingResult =
                rankingEngine.scoreScholarship(scholarship, null, student, null, fixedToday);

        assertFalse(missingResult.isEligible(), "Missing evaluation must never be eligible");
        assertEquals(new BigDecimal("0.00"), missingResult.matchScore());
        assertEquals(new BigDecimal("0.00"), missingResult.eligibilityScore());
        assertNull(missingResult.verdict());
        assertTrue(missingResult.breakdown().explanation().contains("Ineligible"));

        // 2. INELIGIBLE evaluation
        BatchEvaluateItemDto ineligibleEval = new BatchEvaluateItemDto(
                201L, 1, "INELIGIBLE", new BigDecimal("30.00"), 1, 4, false
        );
        RecommendationRankingEngine.RankedResult ineligibleResult =
                rankingEngine.scoreScholarship(scholarship, ineligibleEval, student, null, fixedToday);

        assertFalse(ineligibleResult.isEligible(), "INELIGIBLE evaluation must never be recommended");
        assertEquals(new BigDecimal("0.00"), ineligibleResult.matchScore());
        assertEquals(new BigDecimal("0.00"), ineligibleResult.eligibilityScore());
        assertNull(ineligibleResult.verdict());
    }

    @Test
    @DisplayName("Eligible scholarship produces composite score, correct breakdown, and human-readable explanation")
    void testEligibleScholarshipRanking() {
        ScholarshipSummaryClientDto scholarship = new ScholarshipSummaryClientDto(
                301L, "tech-leaders", "Tech Leaders Grant", "Silicon Valley Trust", "FOUNDATION",
                new BigDecimal("10000"), new BigDecimal("10000"), "USD", "ONE_TIME",
                false, fixedToday.minusDays(10), fixedToday.plusDays(20), false,
                List.of("US"), List.of("UNDERGRADUATE"), new BigDecimal("3.00"),
                "VERIFIED", "PUBLISHED"
        );

        StudentProfileClientDto student = new StudentProfileClientDto(
                UUID.randomUUID(), UUID.randomUUID(), "Sam", "Alt", "Sam Alt",
                LocalDate.of(2003, 7, 7), "US", "CA", "UNDERGRADUATE",
                "Berkeley", "Computer Science", "Junior",
                new BigDecimal("3.95"), new BigDecimal("4.00"), null,
                "USD", false, false, false, null
        );

        BatchEvaluateItemDto eligibleEval = new BatchEvaluateItemDto(
                301L, 2, "ELIGIBLE", new BigDecimal("100.00"), 4, 4, false
        );

        RecommendationRankingEngine.RankedResult result =
                rankingEngine.scoreScholarship(scholarship, eligibleEval, student, null, fixedToday);

        assertTrue(result.isEligible());
        assertEquals(EligibilityVerdict.ELIGIBLE, result.verdict());
        assertEquals(new BigDecimal("100.00"), result.eligibilityScore());
        assertEquals(2, result.ruleSetVersion());

        // Composite match score:
        // Eligibility = 100 * 0.50 = 50.0
        // Academic = (50 base + 20 edLevel + 15 no major in title/prov) = 85 * 0.20 = 17.0
        // Urgency = 20 days (approaching) = 85 * 0.15 = 12.75
        // Award = $10,000 = 100 * 0.15 = 15.0
        // Total = 50 + 17 + 12.75 + 15 = 94.75
        assertEquals(new BigDecimal("94.75"), result.matchScore());

        // Breakdown verification
        assertNotNull(result.breakdown());
        assertEquals(new BigDecimal("100.00"), result.breakdown().eligibilityScore());
        assertEquals(new BigDecimal("85.00"), result.breakdown().academicAffinityScore());
        assertEquals(new BigDecimal("85.00"), result.breakdown().deadlineUrgencyScore());
        assertEquals(new BigDecimal("100.00"), result.breakdown().awardFactorScore());
        assertTrue(result.breakdown().explanation().contains("Deterministic eligibility verified"));
        assertTrue(result.breakdown().explanation().contains("High-value award opportunity ($10000)"));
    }

    @Test
    @DisplayName("Needs verification scholarship is scored with verdict NEEDS_VERIFICATION and appropriate explanation")
    void testNeedsVerificationScholarshipRanking() {
        ScholarshipSummaryClientDto scholarship = new ScholarshipSummaryClientDto(
                401L, "community-grant", "Community Impact Grant", "Local Org", "CORPORATE",
                new BigDecimal("2500"), new BigDecimal("2500"), "USD", "ONE_TIME",
                false, fixedToday.minusDays(5), fixedToday.plusDays(50), false,
                List.of("US"), List.of("UNDERGRADUATE"), new BigDecimal("2.50"),
                "VERIFIED", "PUBLISHED"
        );

        StudentProfileClientDto student = new StudentProfileClientDto(
                UUID.randomUUID(), UUID.randomUUID(), "Eva", "Green", "Eva Green",
                LocalDate.of(2004, 2, 2), "US", "TX", "UNDERGRADUATE",
                "UT Austin", "History", "Freshman",
                new BigDecimal("3.20"), new BigDecimal("4.00"), null,
                "USD", false, false, false, null
        );

        BatchEvaluateItemDto eval = new BatchEvaluateItemDto(
                401L, 1, "NEEDS_VERIFICATION", new BigDecimal("70.00"), 2, 3, true
        );

        RecommendationRankingEngine.RankedResult result =
                rankingEngine.scoreScholarship(scholarship, eval, student, null, fixedToday);

        assertTrue(result.isEligible());
        assertEquals(EligibilityVerdict.NEEDS_VERIFICATION, result.verdict());
        assertEquals(new BigDecimal("70.00"), result.eligibilityScore());
        assertTrue(result.breakdown().explanation().contains("Requires profile verification for some criteria."));
    }
}
