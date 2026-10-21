package org.prestiq.eligibility.service;

import org.prestiq.eligibility.client.StudentServiceClient;
import org.prestiq.eligibility.domain.entity.EligibilityEvaluation;
import org.prestiq.eligibility.domain.entity.RuleSetStatus;
import org.prestiq.eligibility.domain.entity.ScholarshipRuleSet;
import org.prestiq.eligibility.domain.entity.Verdict;
import org.prestiq.eligibility.dto.request.BatchEvaluateRequest;
import org.prestiq.eligibility.dto.response.BatchEvaluateItemResponse;
import org.prestiq.eligibility.dto.response.BatchEvaluateResponse;
import org.prestiq.eligibility.dto.response.EligibilityEvaluationResponse;
import org.prestiq.eligibility.dto.response.StudentProfileDto;
import org.prestiq.eligibility.engine.DeterministicEligibilityEngine;
import org.prestiq.eligibility.exception.EvaluationNotFoundException;
import org.prestiq.eligibility.exception.RuleSetNotFoundException;
import org.prestiq.eligibility.repository.EligibilityEvaluationRepository;
import org.prestiq.eligibility.repository.ScholarshipRuleSetRepository;
import org.prestiq.eligibility.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EligibilityServiceImpl implements EligibilityService {

    private static final Logger log = LoggerFactory.getLogger(EligibilityServiceImpl.class);

    private final ScholarshipRuleSetRepository ruleSetRepository;
    private final EligibilityEvaluationRepository evaluationRepository;
    private final DeterministicEligibilityEngine engine;
    private final StudentServiceClient studentServiceClient;

    public EligibilityServiceImpl(
            ScholarshipRuleSetRepository ruleSetRepository,
            EligibilityEvaluationRepository evaluationRepository,
            DeterministicEligibilityEngine engine,
            StudentServiceClient studentServiceClient
    ) {
        this.ruleSetRepository = ruleSetRepository;
        this.evaluationRepository = evaluationRepository;
        this.engine = engine;
        this.studentServiceClient = studentServiceClient;
    }

    @Override
    @Transactional
    public EligibilityEvaluationResponse evaluateStudent(
            UUID studentId,
            Long scholarshipId,
            StudentProfileDto profileOverride
    ) {
        log.info("Evaluating eligibility for student {} against scholarship {}", studentId, scholarshipId);

        StudentProfileDto profile = profileOverride;
        if (profile == null) {
            profile = studentServiceClient.fetchStudentProfile(studentId);
        }

        ScholarshipRuleSet ruleSet = ruleSetRepository.findActiveWithCriteria(scholarshipId, RuleSetStatus.ACTIVE)
                .orElseThrow(() -> new RuleSetNotFoundException("No active eligibility rule set found for scholarship ID: " + scholarshipId));

        DeterministicEligibilityEngine.EngineEvaluationResult result = engine.evaluate(ruleSet, profile);

        EligibilityEvaluation evaluation = new EligibilityEvaluation(
                UUID.randomUUID(),
                studentId,
                scholarshipId,
                ruleSet.getVersion(),
                result.verdict(),
                result.matchPercentage(),
                result.breakdown(),
                Instant.now()
        );

        EligibilityEvaluation saved = evaluationRepository.save(evaluation);
        log.info("Evaluation persisted with ID: {}, verdict: {}, matchPercentage: {}%",
                saved.getId(), saved.getVerdict(), saved.getMatchPercentage());

        return EligibilityEvaluationResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EligibilityEvaluationResponse getEvaluationById(UUID evaluationId, UserPrincipal principal) {
        EligibilityEvaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new EvaluationNotFoundException("Evaluation record not found for ID: " + evaluationId));

        // Enforce student ownership and cross-user isolation
        if (principal != null && !principal.isInternal()) {
            boolean isAdmin = principal.hasRole("ROLE_ADMIN") || principal.hasRole("ADMIN")
                    || principal.hasRole("ROLE_SCHOLARSHIP_OFFICER") || principal.hasRole("SCHOLARSHIP_OFFICER");

            if (!isAdmin && !evaluation.getStudentId().equals(principal.getId())) {
                log.warn("Cross-user access blocked: User {} attempted to view evaluation {} belonging to student {}",
                        principal.getId(), evaluationId, evaluation.getStudentId());
                throw new AccessDeniedException("Access denied: You are not authorized to view evaluation records of other students.");
            }
        }

        return EligibilityEvaluationResponse.fromEntity(evaluation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EligibilityEvaluationResponse> getMyEvaluations(UUID studentId) {
        return evaluationRepository.findByStudentIdOrderByEvaluatedAtDesc(studentId)
                .stream()
                .map(EligibilityEvaluationResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BatchEvaluateResponse batchEvaluate(BatchEvaluateRequest request) {
        log.info("Batch evaluating {} scholarships for student", request.scholarshipIds().size());

        List<ScholarshipRuleSet> activeRuleSets = ruleSetRepository.findActiveWithCriteriaForScholarships(request.scholarshipIds());
        Map<Long, ScholarshipRuleSet> ruleSetMap = activeRuleSets.stream()
                .collect(Collectors.toMap(ScholarshipRuleSet::getScholarshipId, Function.identity(), (r1, r2) -> r1));

        List<BatchEvaluateItemResponse> items = new ArrayList<>();

        for (Long scholarshipId : request.scholarshipIds()) {
            ScholarshipRuleSet ruleSet = ruleSetMap.get(scholarshipId);
            if (ruleSet == null) {
                // No active rule set defined for this scholarship yet -> consider NOT_EVALUATED or NEEDS_VERIFICATION with 0%
                items.add(new BatchEvaluateItemResponse(
                        scholarshipId,
                        0,
                        Verdict.NEEDS_VERIFICATION,
                        BigDecimal.ZERO.setScale(2),
                        0,
                        0,
                        true
                ));
                continue;
            }

            DeterministicEligibilityEngine.EngineEvaluationResult result = engine.evaluate(ruleSet, request.studentProfile());
            items.add(new BatchEvaluateItemResponse(
                    scholarshipId,
                    ruleSet.getVersion(),
                    result.verdict(),
                    result.matchPercentage(),
                    result.passedCriteriaCount(),
                    result.totalCriteriaCount(),
                    result.hasMissingData()
            ));
        }

        return new BatchEvaluateResponse(items);
    }
}
