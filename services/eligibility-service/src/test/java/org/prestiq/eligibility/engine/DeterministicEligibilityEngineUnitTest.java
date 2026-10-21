package org.prestiq.eligibility.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.prestiq.eligibility.domain.entity.CriterionOperator;
import org.prestiq.eligibility.domain.entity.CriterionStatus;
import org.prestiq.eligibility.domain.entity.EligibilityCriterion;
import org.prestiq.eligibility.domain.entity.RuleSetStatus;
import org.prestiq.eligibility.domain.entity.ScholarshipRuleSet;
import org.prestiq.eligibility.domain.entity.Verdict;
import org.prestiq.eligibility.dto.response.StudentProfileDto;
import org.prestiq.eligibility.exception.InvalidCriterionException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Deterministic Eligibility Engine Unit Tests")
class DeterministicEligibilityEngineUnitTest {

    private DeterministicEligibilityEngine engine;
    private StudentFieldResolver fieldResolver;
    private EvaluatorRegistry evaluatorRegistry;

    @BeforeEach
    void setUp() {
        fieldResolver = new StudentFieldResolver();
        List<CriterionEvaluator> evaluators = List.of(
                new NumericRangeEvaluator(),
                new SetInclusionEvaluator(),
                new BooleanFlagEvaluator(),
                new StringMatchEvaluator()
        );
        evaluatorRegistry = new EvaluatorRegistry(evaluators, fieldResolver);
        engine = new DeterministicEligibilityEngine(fieldResolver, evaluatorRegistry);
    }

    private StudentProfileDto createBaseStudentProfile() {
        return new StudentProfileDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "John",
                "Doe",
                "John Doe",
                LocalDate.of(2003, 5, 15),
                "US",
                "CA",
                "UNDERGRADUATE",
                "Stanford University",
                "Computer Science",
                "Junior",
                BigDecimal.valueOf(3.85),
                BigDecimal.valueOf(4.00),
                BigDecimal.valueOf(45000.00),
                "USD",
                true,
                false,
                false,
                Map.of("athlete", true)
        );
    }

    @Test
    @DisplayName("Should evaluate ELIGIBLE when all mandatory criteria pass")
    void shouldEvaluateEligibleWhenAllMandatoryCriteriaPass() {
        ScholarshipRuleSet ruleSet = new ScholarshipRuleSet(101L, 1, RuleSetStatus.ACTIVE, UUID.randomUUID());

        EligibilityCriterion gpaCrit = new EligibilityCriterion(
                "MIN_GPA", "academic.cumulativeGpa", CriterionOperator.GTE, "3.50",
                true, 0, "Minimum Cumulative GPA", null, null
        );
        EligibilityCriterion countryCrit = new EligibilityCriterion(
                "CITIZENSHIP", "demographics.citizenshipCountry", CriterionOperator.IN, "[\"US\", \"CA\"]",
                true, 0, "Eligible Countries", null, null
        );

        ruleSet.addCriterion(gpaCrit);
        ruleSet.addCriterion(countryCrit);

        StudentProfileDto student = createBaseStudentProfile();

        DeterministicEligibilityEngine.EngineEvaluationResult result = engine.evaluate(ruleSet, student);

        assertEquals(Verdict.ELIGIBLE, result.verdict());
        assertEquals(BigDecimal.valueOf(100.00).setScale(2), result.matchPercentage());
        assertEquals(2, result.breakdown().size());
        assertTrue(result.breakdown().stream().allMatch(c -> c.status() == CriterionStatus.PASS));
    }

    @ParameterizedTest(name = "GPA boundary test: student GPA {0} vs required 3.50 -> expected status {1}")
    @CsvSource({
            "3.50, PASS",
            "3.5001, PASS",
            "3.4999, FAIL",
            "3.20, FAIL",
            "4.00, PASS"
    })
    @DisplayName("Should test exact boundary values for GPA threshold")
    void shouldTestGpaBoundaryValues(String gpaStr, String expectedStatusStr) {
        ScholarshipRuleSet ruleSet = new ScholarshipRuleSet(101L, 1, RuleSetStatus.ACTIVE, UUID.randomUUID());
        EligibilityCriterion gpaCrit = new EligibilityCriterion(
                "MIN_GPA", "academic.cumulativeGpa", CriterionOperator.GTE, "3.50",
                true, 0, "Minimum Cumulative GPA", null, null
        );
        ruleSet.addCriterion(gpaCrit);

        StudentProfileDto student = new StudentProfileDto(
                UUID.randomUUID(), UUID.randomUUID(), "Jane", "Doe", "Jane Doe",
                LocalDate.of(2003, 1, 1), "US", "NY", "UNDERGRADUATE", "MIT",
                "Computer Science", "Sophomore", new BigDecimal(gpaStr),
                BigDecimal.valueOf(4.00), BigDecimal.valueOf(50000.00), "USD",
                false, false, false, Map.of()
        );

        DeterministicEligibilityEngine.EngineEvaluationResult result = engine.evaluate(ruleSet, student);
        CriterionStatus expected = CriterionStatus.valueOf(expectedStatusStr);

        assertEquals(expected, result.breakdown().getFirst().status());
        if (expected == CriterionStatus.PASS) {
            assertEquals(Verdict.ELIGIBLE, result.verdict());
        } else {
            assertEquals(Verdict.INELIGIBLE, result.verdict());
        }
    }

    @Test
    @DisplayName("Should correctly normalize student GPA from 10.0 scale to 4.0 scale")
    void shouldNormalizeGpaScaleAccurately() {
        ScholarshipRuleSet ruleSet = new ScholarshipRuleSet(101L, 1, RuleSetStatus.ACTIVE, UUID.randomUUID());
        EligibilityCriterion gpaCrit = new EligibilityCriterion(
                "MIN_GPA", "academic.cumulativeGpa", CriterionOperator.GTE, "3.50",
                true, 0, "Minimum Cumulative GPA", null, null
        );
        ruleSet.addCriterion(gpaCrit);

        // Student from India with 8.75 / 10.0 scale: (8.75 / 10) * 4.0 = 3.50 -> PASS!
        StudentProfileDto passingStudent = new StudentProfileDto(
                UUID.randomUUID(), UUID.randomUUID(), "Arjun", "Kumar", "Arjun Kumar",
                LocalDate.of(2002, 3, 1), "IN", "Delhi", "UNDERGRADUATE", "IIT",
                "Engineering", "Final Year", BigDecimal.valueOf(8.75),
                BigDecimal.valueOf(10.00), BigDecimal.valueOf(15000.00), "USD",
                false, false, false, Map.of()
        );

        DeterministicEligibilityEngine.EngineEvaluationResult passingResult = engine.evaluate(ruleSet, passingStudent);
        assertEquals(CriterionStatus.PASS, passingResult.breakdown().getFirst().status());
        assertEquals(Verdict.ELIGIBLE, passingResult.verdict());

        // Student with 8.50 / 10.0 scale: (8.50 / 10) * 4.0 = 3.40 -> FAIL!
        StudentProfileDto failingStudent = new StudentProfileDto(
                UUID.randomUUID(), UUID.randomUUID(), "Vikram", "Sharma", "Vikram Sharma",
                LocalDate.of(2002, 3, 1), "IN", "Delhi", "UNDERGRADUATE", "IIT",
                "Engineering", "Final Year", BigDecimal.valueOf(8.50),
                BigDecimal.valueOf(10.00), BigDecimal.valueOf(15000.00), "USD",
                false, false, false, Map.of()
        );

        DeterministicEligibilityEngine.EngineEvaluationResult failingResult = engine.evaluate(ruleSet, failingStudent);
        assertEquals(CriterionStatus.FAIL, failingResult.breakdown().getFirst().status());
        assertEquals(Verdict.INELIGIBLE, failingResult.verdict());
    }

    @ParameterizedTest(name = "Income boundary: student income {0} vs max 65000 -> expected {1}")
    @CsvSource({
            "50000.00, PASS",
            "65000.00, PASS",
            "65000.01, FAIL",
            "70000.00, FAIL"
    })
    @DisplayName("Should test exact boundary values for household income")
    void shouldTestIncomeBoundaryValues(String incomeStr, String expectedStatusStr) {
        ScholarshipRuleSet ruleSet = new ScholarshipRuleSet(101L, 1, RuleSetStatus.ACTIVE, UUID.randomUUID());
        EligibilityCriterion incomeCrit = new EligibilityCriterion(
                "MAX_INCOME", "financial.annualFamilyIncome", CriterionOperator.LTE, "65000.00",
                true, 0, "Maximum Family Income", null, null
        );
        ruleSet.addCriterion(incomeCrit);

        StudentProfileDto student = new StudentProfileDto(
                UUID.randomUUID(), UUID.randomUUID(), "Alice", "Smith", "Alice Smith",
                LocalDate.of(2003, 1, 1), "US", "TX", "UNDERGRADUATE", "UT Austin",
                "Biology", "Freshman", BigDecimal.valueOf(3.70),
                BigDecimal.valueOf(4.00), new BigDecimal(incomeStr), "USD",
                false, false, false, Map.of()
        );

        DeterministicEligibilityEngine.EngineEvaluationResult result = engine.evaluate(ruleSet, student);
        CriterionStatus expected = CriterionStatus.valueOf(expectedStatusStr);

        assertEquals(expected, result.breakdown().getFirst().status());
    }

    @Test
    @DisplayName("Should evaluate BETWEEN operator for income range")
    void shouldEvaluateBetweenOperator() {
        ScholarshipRuleSet ruleSet = new ScholarshipRuleSet(101L, 1, RuleSetStatus.ACTIVE, UUID.randomUUID());
        EligibilityCriterion rangeCrit = new EligibilityCriterion(
                "INCOME_BRACKET", "financial.annualFamilyIncome", CriterionOperator.BETWEEN, "{\"min\": 30000, \"max\": 60000}",
                true, 0, "Middle Income Bracket", null, null
        );
        ruleSet.addCriterion(rangeCrit);

        StudentProfileDto studentIn = new StudentProfileDto(
                UUID.randomUUID(), UUID.randomUUID(), "Bob", "Brown", "Bob Brown",
                LocalDate.of(2003, 1, 1), "US", "IL", "UNDERGRADUATE", "UIUC",
                "Physics", "Junior", BigDecimal.valueOf(3.50), BigDecimal.valueOf(4.00),
                BigDecimal.valueOf(45000.00), "USD", false, false, false, Map.of()
        );
        assertEquals(Verdict.ELIGIBLE, engine.evaluate(ruleSet, studentIn).verdict());

        StudentProfileDto studentOut = new StudentProfileDto(
                UUID.randomUUID(), UUID.randomUUID(), "Bob", "Brown", "Bob Brown",
                LocalDate.of(2003, 1, 1), "US", "IL", "UNDERGRADUATE", "UIUC",
                "Physics", "Junior", BigDecimal.valueOf(3.50), BigDecimal.valueOf(4.00),
                BigDecimal.valueOf(70000.00), "USD", false, false, false, Map.of()
        );
        assertEquals(Verdict.INELIGIBLE, engine.evaluate(ruleSet, studentOut).verdict());
    }

    @Test
    @DisplayName("Should evaluate Set membership operators (IN, NOT_IN)")
    void shouldEvaluateSetMembershipOperators() {
        ScholarshipRuleSet ruleSet = new ScholarshipRuleSet(101L, 1, RuleSetStatus.ACTIVE, UUID.randomUUID());
        EligibilityCriterion countryCrit = new EligibilityCriterion(
                "CITIZENSHIP", "demographics.citizenshipCountry", CriterionOperator.IN, "[\"US\", \"CA\", \"UK\"]",
                true, 0, "Eligible Countries", null, null
        );
        EligibilityCriterion majorCrit = new EligibilityCriterion(
                "MAJOR", "academic.majorFieldOfStudy", CriterionOperator.IN, "[\"Computer Science\", \"Software Engineering\", \"Data Science\"]",
                true, 0, "STEM Majors", null, null
        );
        ruleSet.addCriterion(countryCrit);
        ruleSet.addCriterion(majorCrit);

        StudentProfileDto matchStudent = createBaseStudentProfile(); // US and Computer Science
        assertEquals(Verdict.ELIGIBLE, engine.evaluate(ruleSet, matchStudent).verdict());

        StudentProfileDto nonMatchMajorStudent = new StudentProfileDto(
                UUID.randomUUID(), UUID.randomUUID(), "Carol", "White", "Carol White",
                LocalDate.of(2003, 1, 1), "US", "CA", "UNDERGRADUATE", "UCLA",
                "History", "Junior", BigDecimal.valueOf(3.85), BigDecimal.valueOf(4.00),
                BigDecimal.valueOf(45000.00), "USD", false, false, false, Map.of()
        );
        assertEquals(Verdict.INELIGIBLE, engine.evaluate(ruleSet, nonMatchMajorStudent).verdict());
    }

    @Test
    @DisplayName("Should evaluate Boolean flag criteria (IS_TRUE, IS_FALSE)")
    void shouldEvaluateBooleanFlagCriteria() {
        ScholarshipRuleSet ruleSet = new ScholarshipRuleSet(101L, 1, RuleSetStatus.ACTIVE, UUID.randomUUID());
        EligibilityCriterion firstGenCrit = new EligibilityCriterion(
                "FIRST_GEN", "demographics.firstGenStudent", CriterionOperator.IS_TRUE, "true",
                true, 0, "First Generation Student", null, null
        );
        ruleSet.addCriterion(firstGenCrit);

        StudentProfileDto firstGenStudent = createBaseStudentProfile(); // firstGenStudent is true
        assertEquals(Verdict.ELIGIBLE, engine.evaluate(ruleSet, firstGenStudent).verdict());

        StudentProfileDto notFirstGenStudent = new StudentProfileDto(
                UUID.randomUUID(), UUID.randomUUID(), "Dave", "Miller", "Dave Miller",
                LocalDate.of(2003, 1, 1), "US", "CA", "UNDERGRADUATE", "Stanford",
                "Computer Science", "Junior", BigDecimal.valueOf(3.85), BigDecimal.valueOf(4.00),
                BigDecimal.valueOf(45000.00), "USD", false, false, false, Map.of()
        );
        assertEquals(Verdict.INELIGIBLE, engine.evaluate(ruleSet, notFirstGenStudent).verdict());
    }

    @Test
    @DisplayName("Should return NEEDS_VERIFICATION when a mandatory field is missing and no criteria fail")
    void shouldReturnNeedsVerificationWhenMandatoryFieldIsMissing() {
        ScholarshipRuleSet ruleSet = new ScholarshipRuleSet(101L, 1, RuleSetStatus.ACTIVE, UUID.randomUUID());
        EligibilityCriterion gpaCrit = new EligibilityCriterion(
                "MIN_GPA", "academic.cumulativeGpa", CriterionOperator.GTE, "3.50",
                true, 0, "Minimum Cumulative GPA", null, null
        );
        EligibilityCriterion countryCrit = new EligibilityCriterion(
                "CITIZENSHIP", "demographics.citizenshipCountry", CriterionOperator.IN, "[\"US\", \"CA\"]",
                true, 0, "Eligible Countries", null, null
        );
        ruleSet.addCriterion(gpaCrit);
        ruleSet.addCriterion(countryCrit);

        // Student with missing GPA (null), but matching country (US)
        StudentProfileDto studentWithMissingGpa = new StudentProfileDto(
                UUID.randomUUID(), UUID.randomUUID(), "Emma", "Watson", "Emma Watson",
                LocalDate.of(2003, 1, 1), "US", "CA", "UNDERGRADUATE", "Stanford",
                "Computer Science", "Junior", null, // Missing GPA
                BigDecimal.valueOf(4.00), BigDecimal.valueOf(45000.00), "USD",
                true, false, false, Map.of()
        );

        DeterministicEligibilityEngine.EngineEvaluationResult result = engine.evaluate(ruleSet, studentWithMissingGpa);

        assertEquals(Verdict.NEEDS_VERIFICATION, result.verdict());
        assertTrue(result.hasMissingData());
        assertEquals(CriterionStatus.MISSING_DATA, result.breakdown().getFirst().status());
        assertEquals(CriterionStatus.PASS, result.breakdown().get(1).status());
        assertNotNull(result.breakdown().getFirst().remediationAction());
    }

    @Test
    @DisplayName("Precedence Rule: INELIGIBLE takes absolute precedence when some criteria fail and others are missing")
    void shouldPrioritizeIneligibleOverNeedsVerificationWhenCriteriaFail() {
        ScholarshipRuleSet ruleSet = new ScholarshipRuleSet(101L, 1, RuleSetStatus.ACTIVE, UUID.randomUUID());
        EligibilityCriterion gpaCrit = new EligibilityCriterion(
                "MIN_GPA", "academic.cumulativeGpa", CriterionOperator.GTE, "3.50",
                true, 0, "Minimum Cumulative GPA", null, null
        );
        EligibilityCriterion countryCrit = new EligibilityCriterion(
                "CITIZENSHIP", "demographics.citizenshipCountry", CriterionOperator.IN, "[\"US\", \"CA\"]",
                true, 0, "Eligible Countries", null, null
        );
        ruleSet.addCriterion(gpaCrit);
        ruleSet.addCriterion(countryCrit);

        // Student with missing GPA (null), BUT country is 'FR' (definite FAIL)
        StudentProfileDto student = new StudentProfileDto(
                UUID.randomUUID(), UUID.randomUUID(), "Jean", "Pierre", "Jean Pierre",
                LocalDate.of(2003, 1, 1), "FR", "Paris", "UNDERGRADUATE", "Sorbonne",
                "Literature", "Junior", null, // Missing GPA
                BigDecimal.valueOf(4.00), BigDecimal.valueOf(45000.00), "USD",
                false, false, false, Map.of()
        );

        DeterministicEligibilityEngine.EngineEvaluationResult result = engine.evaluate(ruleSet, student);

        // Confirmed ineligibility MUST take precedence over missing data!
        assertEquals(Verdict.INELIGIBLE, result.verdict());
        assertEquals(BigDecimal.ZERO.setScale(2), result.matchPercentage());
    }

    @Test
    @DisplayName("Should handle optional / preferred criteria weighting when eligible")
    void shouldHandlePreferredCriteriaWeighting() {
        ScholarshipRuleSet ruleSet = new ScholarshipRuleSet(101L, 1, RuleSetStatus.ACTIVE, UUID.randomUUID());
        EligibilityCriterion mandatoryGpa = new EligibilityCriterion(
                "MIN_GPA", "academic.cumulativeGpa", CriterionOperator.GTE, "3.00",
                true, 0, "Minimum GPA", null, null
        );
        EligibilityCriterion preferredFirstGen = new EligibilityCriterion(
                "FIRST_GEN_PREFERENCE", "demographics.firstGenStudent", CriterionOperator.IS_TRUE, "true",
                false, 20, "First Generation Priority", null, null
        );
        ruleSet.addCriterion(mandatoryGpa);
        ruleSet.addCriterion(preferredFirstGen);

        // Student 1: meets mandatory and preferred -> 100.00%
        StudentProfileDto student1 = createBaseStudentProfile(); // firstGen is true
        DeterministicEligibilityEngine.EngineEvaluationResult res1 = engine.evaluate(ruleSet, student1);
        assertEquals(Verdict.ELIGIBLE, res1.verdict());
        assertEquals(BigDecimal.valueOf(100.00).setScale(2), res1.matchPercentage());

        // Student 2: meets mandatory but NOT preferred -> 80.00%
        StudentProfileDto student2 = new StudentProfileDto(
                UUID.randomUUID(), UUID.randomUUID(), "Frank", "Castle", "Frank Castle",
                LocalDate.of(2003, 1, 1), "US", "CA", "UNDERGRADUATE", "Stanford",
                "Computer Science", "Junior", BigDecimal.valueOf(3.85), BigDecimal.valueOf(4.00),
                BigDecimal.valueOf(45000.00), "USD", false, false, false, Map.of()
        );
        DeterministicEligibilityEngine.EngineEvaluationResult res2 = engine.evaluate(ruleSet, student2);
        assertEquals(Verdict.ELIGIBLE, res2.verdict());
        assertEquals(BigDecimal.valueOf(80.00).setScale(2), res2.matchPercentage());
    }

    @Test
    @DisplayName("Should reject invalid fields not in the allowlist")
    void shouldRejectInvalidFieldNotInAllowlist() {
        EligibilityCriterion invalidFieldCrit = new EligibilityCriterion(
                "SQL_INJECTION", "academic.customSqlExpression", CriterionOperator.GTE, "1",
                true, 0, "Hack Attempt", null, null
        );

        assertThrows(InvalidCriterionException.class, () -> evaluatorRegistry.findEvaluator(invalidFieldCrit));
    }

    @Test
    @DisplayName("Should guarantee reproducibility for identical input and rule set")
    void shouldGuaranteeReproducibility() {
        ScholarshipRuleSet ruleSet = new ScholarshipRuleSet(101L, 2, RuleSetStatus.ACTIVE, UUID.randomUUID());
        ruleSet.addCriterion(new EligibilityCriterion(
                "MIN_GPA", "academic.cumulativeGpa", CriterionOperator.GTE, "3.50",
                true, 0, "GPA", null, null
        ));
        StudentProfileDto student = createBaseStudentProfile();

        DeterministicEligibilityEngine.EngineEvaluationResult run1 = engine.evaluate(ruleSet, student);
        DeterministicEligibilityEngine.EngineEvaluationResult run2 = engine.evaluate(ruleSet, student);

        assertEquals(run1.verdict(), run2.verdict());
        assertEquals(run1.matchPercentage(), run2.matchPercentage());
        assertEquals(run1.breakdown().getFirst().status(), run2.breakdown().getFirst().status());
    }
}
