package org.prestiq.eligibility.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.prestiq.eligibility.domain.entity.CriterionOperator;
import org.prestiq.eligibility.domain.entity.CriterionStatus;
import org.prestiq.eligibility.domain.entity.EligibilityCriterion;
import org.prestiq.eligibility.dto.response.CriterionResultResponse;
import org.prestiq.eligibility.dto.response.StudentProfileDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Comprehensive Criterion Evaluator Isolated Tests")
class CriterionEvaluatorsComprehensiveTest {

    private NumericRangeEvaluator numericEvaluator;
    private SetInclusionEvaluator setEvaluator;
    private BooleanFlagEvaluator booleanEvaluator;
    private StringMatchEvaluator stringEvaluator;
    private StudentProfileDto defaultProfile;

    @BeforeEach
    void setUp() {
        numericEvaluator = new NumericRangeEvaluator();
        setEvaluator = new SetInclusionEvaluator();
        booleanEvaluator = new BooleanFlagEvaluator();
        stringEvaluator = new StringMatchEvaluator();

        defaultProfile = new StudentProfileDto(
                UUID.randomUUID(), UUID.randomUUID(), "Alice", "Test", "Alice Test",
                LocalDate.of(2003, 1, 1), "US", "CA", "UNDERGRADUATE", "Stanford",
                "Computer Science", "Junior", BigDecimal.valueOf(3.80),
                BigDecimal.valueOf(4.00), BigDecimal.valueOf(50000.00), "USD",
                true, false, false, Map.of()
        );
    }

    @Test
    @DisplayName("Numeric evaluator supports GT, LT, LTE, GTE, EQ, BETWEEN")
    void testNumericOperatorSupport() {
        assertTrue(numericEvaluator.supports(CriterionOperator.GT, "academic.cumulativeGpa"));
        assertTrue(numericEvaluator.supports(CriterionOperator.LT, "financial.annualFamilyIncome"));
        assertTrue(numericEvaluator.supports(CriterionOperator.BETWEEN, "financial.annualFamilyIncome"));
        assertFalse(numericEvaluator.supports(CriterionOperator.IN, "academic.cumulativeGpa"));
    }

    @Test
    @DisplayName("Numeric evaluator handles strictly less than (LT) and strictly greater than (GT)")
    void testStrictInequalities() {
        EligibilityCriterion gtCrit = new EligibilityCriterion(
                "MIN_CREDITS", "academic.creditHours", CriterionOperator.GT, "60",
                true, 0, "Credits", null, null
        );

        CriterionResultResponse res60 = numericEvaluator.evaluate(gtCrit, 60, defaultProfile);
        assertEquals(CriterionStatus.FAIL, res60.status());

        CriterionResultResponse res61 = numericEvaluator.evaluate(gtCrit, 61, defaultProfile);
        assertEquals(CriterionStatus.PASS, res61.status());

        EligibilityCriterion ltCrit = new EligibilityCriterion(
                "MAX_INCOME", "financial.annualFamilyIncome", CriterionOperator.LT, "50000",
                true, 0, "Max Income", null, null
        );

        CriterionResultResponse res50k = numericEvaluator.evaluate(ltCrit, new BigDecimal("50000.00"), defaultProfile);
        assertEquals(CriterionStatus.FAIL, res50k.status());

        CriterionResultResponse res49k = numericEvaluator.evaluate(ltCrit, new BigDecimal("49999.99"), defaultProfile);
        assertEquals(CriterionStatus.PASS, res49k.status());
    }

    @Test
    @DisplayName("Numeric evaluator handles custom message templates")
    void testCustomExplanationTemplates() {
        EligibilityCriterion crit = new EligibilityCriterion(
                "GPA", "academic.cumulativeGpa", CriterionOperator.GTE, "3.5",
                true, 0, "GPA",
                "Success! Your GPA of {studentValue} exceeded {targetValue}.",
                "Fail! Needed {targetValue} but got {studentValue}."
        );

        CriterionResultResponse passRes = numericEvaluator.evaluate(crit, new BigDecimal("3.80"), defaultProfile);
        assertTrue(passRes.explanation().contains("Success! Your GPA of 3.8 exceeded 3.5."));

        CriterionResultResponse failRes = numericEvaluator.evaluate(crit, new BigDecimal("3.20"), defaultProfile);
        assertTrue(failRes.explanation().contains("Fail! Needed 3.5 but got 3.2."));
    }

    @Test
    @DisplayName("Set evaluator handles comma-separated target values and whitespace trimming")
    void testSetInclusionFormats() {
        EligibilityCriterion crit = new EligibilityCriterion(
                "STATES", "demographics.stateProvince", CriterionOperator.IN, "CA, NY, TX ",
                true, 0, "Eligible States", null, null
        );

        CriterionResultResponse passRes = setEvaluator.evaluate(crit, " ca ", defaultProfile);
        assertEquals(CriterionStatus.PASS, passRes.status());

        CriterionResultResponse failRes = setEvaluator.evaluate(crit, "WA", defaultProfile);
        assertEquals(CriterionStatus.FAIL, failRes.status());
    }

    @Test
    @DisplayName("Set evaluator handles NOT_IN and CONTAINS_ANY with collections")
    void testSetNotInAndContainsAny() {
        EligibilityCriterion notInCrit = new EligibilityCriterion(
                "EXCLUDED_COUNTRIES", "demographics.citizenshipCountry", CriterionOperator.NOT_IN, "[\"CU\", \"KP\", \"IR\"]",
                true, 0, "Excluded Countries", null, null
        );

        CriterionResultResponse usRes = setEvaluator.evaluate(notInCrit, "US", defaultProfile);
        assertEquals(CriterionStatus.PASS, usRes.status());

        CriterionResultResponse bannedRes = setEvaluator.evaluate(notInCrit, "KP", defaultProfile);
        assertEquals(CriterionStatus.FAIL, bannedRes.status());

        EligibilityCriterion anyCrit = new EligibilityCriterion(
                "MAJORS", "academic.majorFieldOfStudy", CriterionOperator.CONTAINS_ANY, "[\"COMPUTER SCIENCE\", \"DATA SCIENCE\"]",
                true, 0, "Majors", null, null
        );
        CriterionResultResponse listRes = setEvaluator.evaluate(anyCrit, List.of("Computer Science", "Mathematics"), defaultProfile);
        assertEquals(CriterionStatus.PASS, listRes.status());
    }

    @Test
    @DisplayName("Boolean evaluator handles 1/0, yes/no and string representations")
    void testBooleanEvaluatorFormats() {
        EligibilityCriterion crit = new EligibilityCriterion(
                "VET", "demographics.militaryVeteran", CriterionOperator.IS_TRUE, "true",
                true, 0, "Veteran", null, null
        );

        assertEquals(CriterionStatus.PASS, booleanEvaluator.evaluate(crit, "yes", defaultProfile).status());
        assertEquals(CriterionStatus.PASS, booleanEvaluator.evaluate(crit, "1", defaultProfile).status());
        assertEquals(CriterionStatus.PASS, booleanEvaluator.evaluate(crit, true, defaultProfile).status());

        assertEquals(CriterionStatus.FAIL, booleanEvaluator.evaluate(crit, "no", defaultProfile).status());
        assertEquals(CriterionStatus.FAIL, booleanEvaluator.evaluate(crit, "0", defaultProfile).status());
        assertEquals(CriterionStatus.FAIL, booleanEvaluator.evaluate(crit, false, defaultProfile).status());
    }

    @Test
    @DisplayName("String match evaluator handles case insensitivity and NOT_EQUALS")
    void testStringMatchEvaluator() {
        EligibilityCriterion eqCrit = new EligibilityCriterion(
                "INSTITUTION", "academic.institutionName", CriterionOperator.EQUALS, "stanford university",
                true, 0, "Institution", null, null
        );

        CriterionResultResponse matchRes = stringEvaluator.evaluate(eqCrit, "STANFORD UNIVERSITY", defaultProfile);
        assertEquals(CriterionStatus.PASS, matchRes.status());

        CriterionResultResponse nonMatchRes = stringEvaluator.evaluate(eqCrit, "Harvard University", defaultProfile);
        assertEquals(CriterionStatus.FAIL, nonMatchRes.status());

        EligibilityCriterion notEqCrit = new EligibilityCriterion(
                "INSTITUTION", "academic.institutionName", CriterionOperator.NOT_EQUALS, "harvard university",
                true, 0, "Not Harvard", null, null
        );
        assertEquals(CriterionStatus.PASS, stringEvaluator.evaluate(notEqCrit, "Stanford University", defaultProfile).status());
        assertEquals(CriterionStatus.FAIL, stringEvaluator.evaluate(notEqCrit, "Harvard University", defaultProfile).status());
    }
}
