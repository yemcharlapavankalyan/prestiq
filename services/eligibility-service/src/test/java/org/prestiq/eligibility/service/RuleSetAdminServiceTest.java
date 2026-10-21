package org.prestiq.eligibility.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.prestiq.eligibility.domain.entity.CriterionOperator;
import org.prestiq.eligibility.domain.entity.RuleSetStatus;
import org.prestiq.eligibility.domain.entity.ScholarshipRuleSet;
import org.prestiq.eligibility.dto.request.CreateCriterionRequest;
import org.prestiq.eligibility.dto.request.CreateRuleSetRequest;
import org.prestiq.eligibility.dto.response.RuleSetResponse;
import org.prestiq.eligibility.engine.BooleanFlagEvaluator;
import org.prestiq.eligibility.engine.EvaluatorRegistry;
import org.prestiq.eligibility.engine.NumericRangeEvaluator;
import org.prestiq.eligibility.engine.SetInclusionEvaluator;
import org.prestiq.eligibility.engine.StringMatchEvaluator;
import org.prestiq.eligibility.engine.StudentFieldResolver;
import org.prestiq.eligibility.exception.DuplicateRuleVersionException;
import org.prestiq.eligibility.exception.InvalidCriterionException;
import org.prestiq.eligibility.exception.RuleSetNotFoundException;
import org.prestiq.eligibility.repository.ScholarshipRuleSetRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("RuleSetAdminService Unit Tests")
class RuleSetAdminServiceTest {

    private ScholarshipRuleSetRepository ruleSetRepository;
    private RuleSetAdminServiceImpl adminService;
    private StudentFieldResolver fieldResolver;
    private EvaluatorRegistry evaluatorRegistry;

    @BeforeEach
    void setUp() {
        ruleSetRepository = mock(ScholarshipRuleSetRepository.class);
        fieldResolver = new StudentFieldResolver();
        evaluatorRegistry = new EvaluatorRegistry(
                List.of(
                        new NumericRangeEvaluator(),
                        new SetInclusionEvaluator(),
                        new BooleanFlagEvaluator(),
                        new StringMatchEvaluator()
                ),
                fieldResolver
        );
        adminService = new RuleSetAdminServiceImpl(ruleSetRepository, fieldResolver, evaluatorRegistry);
    }

    @Test
    @DisplayName("Should create rule set with auto-incremented version when version is omitted")
    void shouldCreateRuleSetWithAutoIncrementedVersion() {
        Long scholarshipId = 101L;
        UUID adminId = UUID.randomUUID();

        ScholarshipRuleSet existingV1 = new ScholarshipRuleSet(scholarshipId, 1, RuleSetStatus.ACTIVE, adminId);
        when(ruleSetRepository.findFirstByScholarshipIdOrderByVersionDesc(scholarshipId))
                .thenReturn(Optional.of(existingV1));
        when(ruleSetRepository.findByScholarshipIdAndStatus(scholarshipId, RuleSetStatus.ACTIVE))
                .thenReturn(Optional.of(existingV1));

        when(ruleSetRepository.save(any(ScholarshipRuleSet.class))).thenAnswer(invocation -> {
            ScholarshipRuleSet s = invocation.getArgument(0);
            s.setId(201L);
            return s;
        });

        CreateCriterionRequest crit = new CreateCriterionRequest(
                "MIN_GPA", "academic.cumulativeGpa", CriterionOperator.GTE, "3.5",
                true, 0, "Min GPA", null, null
        );
        CreateRuleSetRequest req = new CreateRuleSetRequest(scholarshipId, null, RuleSetStatus.ACTIVE, List.of(crit));

        RuleSetResponse response = adminService.createRuleSet(req, adminId);

        assertNotNull(response);
        assertEquals(2, response.version());
        assertEquals(RuleSetStatus.ACTIVE, response.status());
        assertEquals(RuleSetStatus.SUPERSEDED, existingV1.getStatus());
        verify(ruleSetRepository, times(2)).save(any(ScholarshipRuleSet.class));
    }

    @Test
    @DisplayName("Should throw DuplicateRuleVersionException if version already exists")
    void shouldThrowDuplicateRuleVersionException() {
        Long scholarshipId = 101L;
        when(ruleSetRepository.existsByScholarshipIdAndVersion(scholarshipId, 1)).thenReturn(true);

        CreateCriterionRequest crit = new CreateCriterionRequest(
                "MIN_GPA", "academic.cumulativeGpa", CriterionOperator.GTE, "3.5",
                true, 0, "Min GPA", null, null
        );
        CreateRuleSetRequest req = new CreateRuleSetRequest(scholarshipId, 1, RuleSetStatus.ACTIVE, List.of(crit));

        assertThrows(DuplicateRuleVersionException.class, () -> adminService.createRuleSet(req, UUID.randomUUID()));
    }

    @Test
    @DisplayName("Should throw InvalidCriterionException if target field is not allowlisted")
    void shouldThrowInvalidCriterionExceptionForUnallowlistedField() {
        Long scholarshipId = 101L;

        CreateCriterionRequest crit = new CreateCriterionRequest(
                "HACK", "malicious.expression.field", CriterionOperator.GTE, "1",
                true, 0, "Bad Field", null, null
        );
        CreateRuleSetRequest req = new CreateRuleSetRequest(scholarshipId, 1, RuleSetStatus.ACTIVE, List.of(crit));

        assertThrows(InvalidCriterionException.class, () -> adminService.createRuleSet(req, UUID.randomUUID()));
    }

    @Test
    @DisplayName("Should activate a rule set and supersede previously active version")
    void shouldActivateRuleSetAndSupersedePreviousActive() {
        Long scholarshipId = 101L;
        ScholarshipRuleSet activeV1 = new ScholarshipRuleSet(scholarshipId, 1, RuleSetStatus.ACTIVE, UUID.randomUUID());
        activeV1.setId(10L);

        ScholarshipRuleSet draftV2 = new ScholarshipRuleSet(scholarshipId, 2, RuleSetStatus.DRAFT, UUID.randomUUID());
        draftV2.setId(20L);

        when(ruleSetRepository.findById(20L)).thenReturn(Optional.of(draftV2));
        when(ruleSetRepository.findByScholarshipIdAndStatus(scholarshipId, RuleSetStatus.ACTIVE))
                .thenReturn(Optional.of(activeV1));
        when(ruleSetRepository.save(any(ScholarshipRuleSet.class))).thenAnswer(i -> i.getArgument(0));

        RuleSetResponse response = adminService.activateRuleSet(20L);

        assertEquals(RuleSetStatus.ACTIVE, response.status());
        assertEquals(RuleSetStatus.SUPERSEDED, activeV1.getStatus());
        assertEquals(RuleSetStatus.ACTIVE, draftV2.getStatus());
    }

    @Test
    @DisplayName("Should throw RuleSetNotFoundException when ID does not exist")
    void shouldThrowRuleSetNotFoundException() {
        when(ruleSetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuleSetNotFoundException.class, () -> adminService.getRuleSetById(999L));
        assertThrows(RuleSetNotFoundException.class, () -> adminService.activateRuleSet(999L));
    }
}
