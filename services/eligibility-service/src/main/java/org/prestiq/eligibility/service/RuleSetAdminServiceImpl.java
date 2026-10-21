package org.prestiq.eligibility.service;

import org.prestiq.eligibility.domain.entity.EligibilityCriterion;
import org.prestiq.eligibility.domain.entity.RuleSetStatus;
import org.prestiq.eligibility.domain.entity.ScholarshipRuleSet;
import org.prestiq.eligibility.dto.request.CreateCriterionRequest;
import org.prestiq.eligibility.dto.request.CreateRuleSetRequest;
import org.prestiq.eligibility.dto.response.RuleSetResponse;
import org.prestiq.eligibility.engine.EvaluatorRegistry;
import org.prestiq.eligibility.engine.StudentFieldResolver;
import org.prestiq.eligibility.exception.DuplicateRuleVersionException;
import org.prestiq.eligibility.exception.InvalidCriterionException;
import org.prestiq.eligibility.exception.RuleSetNotFoundException;
import org.prestiq.eligibility.repository.ScholarshipRuleSetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RuleSetAdminServiceImpl implements RuleSetAdminService {

    private static final Logger log = LoggerFactory.getLogger(RuleSetAdminServiceImpl.class);

    private final ScholarshipRuleSetRepository ruleSetRepository;
    private final StudentFieldResolver fieldResolver;
    private final EvaluatorRegistry evaluatorRegistry;

    public RuleSetAdminServiceImpl(
            ScholarshipRuleSetRepository ruleSetRepository,
            StudentFieldResolver fieldResolver,
            EvaluatorRegistry evaluatorRegistry
    ) {
        this.ruleSetRepository = ruleSetRepository;
        this.fieldResolver = fieldResolver;
        this.evaluatorRegistry = evaluatorRegistry;
    }

    @Override
    @Transactional
    public RuleSetResponse createRuleSet(CreateRuleSetRequest request, UUID adminUserId) {
        Long scholarshipId = request.scholarshipId();
        int version;

        if (request.version() != null) {
            version = request.version();
            if (ruleSetRepository.existsByScholarshipIdAndVersion(scholarshipId, version)) {
                throw new DuplicateRuleVersionException("Rule set version " + version + " already exists for scholarship ID " + scholarshipId);
            }
        } else {
            version = ruleSetRepository.findFirstByScholarshipIdOrderByVersionDesc(scholarshipId)
                    .map(r -> r.getVersion() + 1)
                    .orElse(1);
        }

        RuleSetStatus initialStatus = request.getEffectiveStatus();

        // If newly created rule set is ACTIVE, supersede existing active rule sets
        if (initialStatus == RuleSetStatus.ACTIVE) {
            ruleSetRepository.findByScholarshipIdAndStatus(scholarshipId, RuleSetStatus.ACTIVE)
                    .ifPresent(existingActive -> {
                        log.info("Superseding previous active rule set {} (v{}) for scholarship {}",
                                existingActive.getId(), existingActive.getVersion(), scholarshipId);
                        existingActive.setStatus(RuleSetStatus.SUPERSEDED);
                        ruleSetRepository.save(existingActive);
                    });
        }

        ScholarshipRuleSet ruleSet = new ScholarshipRuleSet(
                scholarshipId,
                version,
                initialStatus,
                adminUserId != null ? adminUserId : UUID.randomUUID()
        );

        for (CreateCriterionRequest critReq : request.criteria()) {
            if (!fieldResolver.isAllowedField(critReq.targetField())) {
                throw new InvalidCriterionException("Target field '" + critReq.targetField() + "' is not in the allowlist of evaluatable attributes.");
            }

            EligibilityCriterion criterion = new EligibilityCriterion(
                    critReq.criterionCode(),
                    critReq.targetField(),
                    critReq.operator(),
                    critReq.targetValue(),
                    critReq.getIsMandatory(),
                    critReq.getWeight(),
                    critReq.displayName(),
                    critReq.passExplanationTemplate(),
                    critReq.failExplanationTemplate()
            );

            // Pre-validate that an evaluator exists for this operator and field combination
            evaluatorRegistry.findEvaluator(criterion);

            ruleSet.addCriterion(criterion);
        }

        ScholarshipRuleSet saved = ruleSetRepository.save(ruleSet);
        log.info("Created rule set ID {} (v{}) for scholarship {}", saved.getId(), saved.getVersion(), scholarshipId);

        return RuleSetResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public RuleSetResponse activateRuleSet(Long ruleSetId) {
        ScholarshipRuleSet ruleSet = ruleSetRepository.findById(ruleSetId)
                .orElseThrow(() -> new RuleSetNotFoundException("Rule set not found with ID: " + ruleSetId));

        if (ruleSet.getStatus() == RuleSetStatus.ACTIVE) {
            return RuleSetResponse.fromEntity(ruleSet);
        }

        // Supersede current active rule set for this scholarship
        ruleSetRepository.findByScholarshipIdAndStatus(ruleSet.getScholarshipId(), RuleSetStatus.ACTIVE)
                .ifPresent(existingActive -> {
                    log.info("Superseding previous active rule set {} (v{}) for scholarship {}",
                            existingActive.getId(), existingActive.getVersion(), ruleSet.getScholarshipId());
                    existingActive.setStatus(RuleSetStatus.SUPERSEDED);
                    ruleSetRepository.save(existingActive);
                });

        ruleSet.setStatus(RuleSetStatus.ACTIVE);
        ScholarshipRuleSet saved = ruleSetRepository.save(ruleSet);
        log.info("Activated rule set ID {} (v{}) for scholarship {}", saved.getId(), saved.getVersion(), saved.getScholarshipId());

        return RuleSetResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RuleSetResponse getRuleSetById(Long ruleSetId) {
        ScholarshipRuleSet ruleSet = ruleSetRepository.findById(ruleSetId)
                .orElseThrow(() -> new RuleSetNotFoundException("Rule set not found with ID: " + ruleSetId));
        return RuleSetResponse.fromEntity(ruleSet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleSetResponse> getRuleSetsByScholarshipId(Long scholarshipId) {
        return ruleSetRepository.findByScholarshipIdOrderByVersionDesc(scholarshipId)
                .stream()
                .map(RuleSetResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RuleSetResponse getActiveRuleSetForScholarship(Long scholarshipId) {
        ScholarshipRuleSet ruleSet = ruleSetRepository.findActiveWithCriteria(scholarshipId, RuleSetStatus.ACTIVE)
                .orElseThrow(() -> new RuleSetNotFoundException("No active rule set found for scholarship ID: " + scholarshipId));
        return RuleSetResponse.fromEntity(ruleSet);
    }
}
