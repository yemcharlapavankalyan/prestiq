package org.prestiq.recommendation.service;

import org.prestiq.recommendation.client.EligibilityServiceClient;
import org.prestiq.recommendation.client.ScholarshipServiceClient;
import org.prestiq.recommendation.client.StudentServiceClient;
import org.prestiq.recommendation.domain.entity.EligibilityVerdict;
import org.prestiq.recommendation.domain.entity.StudentPreference;
import org.prestiq.recommendation.domain.entity.StudentRecommendation;
import org.prestiq.recommendation.dto.client.BatchEvaluateItemDto;
import org.prestiq.recommendation.dto.client.ScholarshipSummaryClientDto;
import org.prestiq.recommendation.dto.client.StudentProfileClientDto;
import org.prestiq.recommendation.dto.request.UpdatePreferencesRequest;
import org.prestiq.recommendation.dto.response.PagedResponse;
import org.prestiq.recommendation.dto.response.ScholarshipRecommendationResponse;
import org.prestiq.recommendation.dto.response.StudentPreferenceResponse;
import org.prestiq.recommendation.engine.RecommendationRankingEngine;
import org.prestiq.recommendation.exception.RecommendationNotFoundException;
import org.prestiq.recommendation.repository.StudentPreferenceRepository;
import org.prestiq.recommendation.repository.StudentRecommendationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationServiceImpl.class);
    private static final int DEFAULT_SCHOLARSHIP_BATCH_LIMIT = 100;

    private final StudentRecommendationRepository recommendationRepository;
    private final StudentPreferenceRepository preferenceRepository;
    private final StudentServiceClient studentServiceClient;
    private final ScholarshipServiceClient scholarshipServiceClient;
    private final EligibilityServiceClient eligibilityServiceClient;
    private final RecommendationRankingEngine rankingEngine;

    public RecommendationServiceImpl(
            StudentRecommendationRepository recommendationRepository,
            StudentPreferenceRepository preferenceRepository,
            StudentServiceClient studentServiceClient,
            ScholarshipServiceClient scholarshipServiceClient,
            EligibilityServiceClient eligibilityServiceClient,
            RecommendationRankingEngine rankingEngine
    ) {
        this.recommendationRepository = recommendationRepository;
        this.preferenceRepository = preferenceRepository;
        this.studentServiceClient = studentServiceClient;
        this.scholarshipServiceClient = scholarshipServiceClient;
        this.eligibilityServiceClient = eligibilityServiceClient;
        this.rankingEngine = rankingEngine;
    }

    @Override
    @Transactional
    public PagedResponse<ScholarshipRecommendationResponse> getRecommendations(
            UUID studentId,
            int page,
            int size,
            BigDecimal minScore,
            Boolean includeNeedsVerification,
            boolean recalculate
    ) {
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;
        BigDecimal effectiveMinScore = (minScore != null) ? minScore : BigDecimal.ZERO;
        boolean withNeedsVerification = (includeNeedsVerification == null) || includeNeedsVerification;

        // Auto-calculate on initial request or when explicit recalculation is requested
        if (recalculate || recommendationRepository.countByStudentId(studentId) == 0) {
            log.info("Calculating personalized recommendations for student: {}", studentId);
            recalculateRecommendationsInternal(studentId);
        }

        List<EligibilityVerdict> allowedVerdicts = withNeedsVerification
                ? List.of(EligibilityVerdict.ELIGIBLE, EligibilityVerdict.NEEDS_VERIFICATION)
                : List.of(EligibilityVerdict.ELIGIBLE);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("rankPosition"), Sort.Order.asc("scholarshipId"))
        );

        Page<StudentRecommendation> recPage = recommendationRepository
                .findByStudentIdAndIsDismissedFalseAndEligibilityVerdictInAndMatchScoreGreaterThanEqual(
                        studentId,
                        allowedVerdicts,
                        effectiveMinScore,
                        pageable
                );

        if (recPage.isEmpty()) {
            return PagedResponse.of(List.of(), page, size, recPage.getTotalElements());
        }

        // Fetch scholarship details to enrich response
        Map<Long, ScholarshipSummaryClientDto> scholarshipLookup = fetchScholarshipsForRecommendations(recPage.getContent());

        LocalDate today = LocalDate.now();
        List<ScholarshipRecommendationResponse> content = recPage.getContent().stream()
                .map(rec -> toResponse(rec, scholarshipLookup.get(rec.getScholarshipId()), today))
                .toList();

        return PagedResponse.of(content, recPage.getNumber(), recPage.getSize(), recPage.getTotalElements());
    }

    @Override
    @Transactional
    public void dismissRecommendation(UUID studentId, Long scholarshipId) {
        StudentRecommendation rec = recommendationRepository
                .findByStudentIdAndScholarshipId(studentId, scholarshipId)
                .orElseThrow(() -> new RecommendationNotFoundException(
                        "Recommendation not found for scholarship ID: " + scholarshipId));

        rec.setDismissed(true);
        recommendationRepository.save(rec);
        log.info("Dismissed recommendation scholarship {} for student {}", scholarshipId, studentId);
    }

    @Override
    @Transactional
    public void undismissRecommendation(UUID studentId, Long scholarshipId) {
        StudentRecommendation rec = recommendationRepository
                .findByStudentIdAndScholarshipId(studentId, scholarshipId)
                .orElseThrow(() -> new RecommendationNotFoundException(
                        "Recommendation not found for scholarship ID: " + scholarshipId));

        rec.setDismissed(false);
        recommendationRepository.save(rec);
        log.info("Restored recommendation scholarship {} for student {}", scholarshipId, studentId);
    }

    @Override
    @Transactional
    public void recalculateRecommendations(UUID studentId) {
        recalculateRecommendationsInternal(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentPreferenceResponse getPreferences(UUID studentId) {
        return preferenceRepository.findByStudentId(studentId)
                .map(StudentPreferenceResponse::fromEntity)
                .orElseGet(() -> new StudentPreferenceResponse(
                        null,
                        studentId,
                        List.of(),
                        List.of(),
                        null,
                        Instant.now()
                ));
    }

    @Override
    @Transactional
    public StudentPreferenceResponse updatePreferences(UUID studentId, UpdatePreferencesRequest request) {
        StudentPreference preference = preferenceRepository.findByStudentId(studentId)
                .orElseGet(() -> new StudentPreference(
                        UUID.randomUUID(),
                        studentId,
                        new ArrayList<>(),
                        new ArrayList<>(),
                        null,
                        Instant.now()
                ));

        preference.setPreferredFields(request.preferredFields() != null ? request.preferredFields() : List.of());
        preference.setPreferredLocations(request.preferredLocations() != null ? request.preferredLocations() : List.of());
        preference.setMinAwardPreference(request.minAwardPreference());
        preference.setUpdatedAt(Instant.now());

        StudentPreference saved = preferenceRepository.save(preference);
        log.info("Updated preferences for student: {}", studentId);

        // Recalculate recommendations using updated preferences
        try {
            recalculateRecommendationsInternal(studentId);
        } catch (Exception e) {
            log.warn("Could not immediately recalculate recommendations after updating preferences for student {}: {}",
                    studentId, e.getMessage());
        }

        return StudentPreferenceResponse.fromEntity(saved);
    }

    private void recalculateRecommendationsInternal(UUID studentId) {
        // 1. Retrieve authorized student profile from Student Service
        StudentProfileClientDto studentProfile = studentServiceClient.fetchStudentProfile(studentId);

        // 2. Retrieve student preferences
        StudentPreference preferences = preferenceRepository.findByStudentId(studentId).orElse(null);

        // 3. Retrieve published, verified, unexpired scholarships from Scholarship Service
        List<ScholarshipSummaryClientDto> activeScholarships =
                scholarshipServiceClient.fetchActivePublishedScholarships(DEFAULT_SCHOLARSHIP_BATCH_LIMIT);

        if (activeScholarships.isEmpty()) {
            log.info("No active scholarships available for recommendation scoring for student: {}", studentId);
            return;
        }

        List<Long> scholarshipIds = activeScholarships.stream()
                .map(ScholarshipSummaryClientDto::id)
                .toList();

        // 4. Deterministic evaluation with Eligibility Service
        Map<Long, BatchEvaluateItemDto> evaluationMap =
                eligibilityServiceClient.batchEvaluate(studentProfile, scholarshipIds);

        LocalDate today = LocalDate.now();
        List<RecommendationRankingEngine.RankedResult> rankedList = new ArrayList<>();

        // 5. Ranking & Strict Ineligibility Gate
        for (ScholarshipSummaryClientDto scholarship : activeScholarships) {
            BatchEvaluateItemDto evaluation = evaluationMap.get(scholarship.id());

            // Strict Gate: Missing evaluation or INELIGIBLE verdict must NEVER be recommended
            if (evaluation == null || "INELIGIBLE".equalsIgnoreCase(evaluation.verdict())) {
                continue;
            }

            RecommendationRankingEngine.RankedResult ranked =
                    rankingEngine.scoreScholarship(scholarship, evaluation, studentProfile, preferences, today);

            if (ranked.isEligible()) {
                rankedList.add(ranked);
            }
        }

        // 6. Stable, deterministic sort:
        // Primary: matchScore DESC
        // Secondary: upcoming deadline urgency (earlier deadlines rank first)
        // Tertiary: scholarshipId ASC (tie-breaker for absolute stability)
        Map<Long, ScholarshipSummaryClientDto> scholarshipById = activeScholarships.stream()
                .collect(Collectors.toMap(ScholarshipSummaryClientDto::id, s -> s, (s1, s2) -> s1));

        rankedList.sort(Comparator
                .comparing(RecommendationRankingEngine.RankedResult::matchScore, Comparator.reverseOrder())
                .thenComparing(r -> {
                    ScholarshipSummaryClientDto s = scholarshipById.get(r.scholarshipId());
                    return (s != null && s.applicationDeadline() != null)
                            ? ChronoUnit.DAYS.between(today, s.applicationDeadline())
                            : Long.MAX_VALUE;
                })
                .thenComparing(RecommendationRankingEngine.RankedResult::scholarshipId)
        );

        // 7. Load existing recommendation records to preserve dismissal states
        Map<Long, StudentRecommendation> existingByScholarship = recommendationRepository
                .findByStudentId(studentId).stream()
                .collect(Collectors.toMap(StudentRecommendation::getScholarshipId, r -> r, (r1, r2) -> r1));

        List<StudentRecommendation> toPersist = new ArrayList<>();
        for (int i = 0; i < rankedList.size(); i++) {
            RecommendationRankingEngine.RankedResult ranked = rankedList.get(i);
            int rankPosition = i + 1;

            StudentRecommendation existing = existingByScholarship.get(ranked.scholarshipId());
            boolean isDismissed = (existing != null) && existing.isDismissed();
            UUID recId = (existing != null) ? existing.getId() : UUID.randomUUID();

            StudentRecommendation rec = new StudentRecommendation(
                    recId,
                    studentId,
                    ranked.scholarshipId(),
                    ranked.matchScore(),
                    ranked.verdict(),
                    ranked.eligibilityScore(),
                    rankPosition,
                    ranked.breakdown(),
                    ranked.ruleSetVersion(),
                    isDismissed,
                    Instant.now()
            );
            toPersist.add(rec);
        }

        recommendationRepository.saveAll(toPersist);
        log.info("Persisted {} ranked recommendations for student: {}", toPersist.size(), studentId);
    }

    private Map<Long, ScholarshipSummaryClientDto> fetchScholarshipsForRecommendations(
            List<StudentRecommendation> recommendations
    ) {
        // Fetch active published scholarships in bulk
        List<ScholarshipSummaryClientDto> activeList =
                scholarshipServiceClient.fetchActivePublishedScholarships(DEFAULT_SCHOLARSHIP_BATCH_LIMIT);

        Map<Long, ScholarshipSummaryClientDto> map = activeList.stream()
                .collect(Collectors.toMap(ScholarshipSummaryClientDto::id, s -> s, (s1, s2) -> s1));

        // For any recommendations not in active batch, fetch individually
        for (StudentRecommendation rec : recommendations) {
            if (!map.containsKey(rec.getScholarshipId())) {
                scholarshipServiceClient.fetchScholarshipById(rec.getScholarshipId())
                        .ifPresent(dto -> map.put(dto.id(), dto));
            }
        }

        return map;
    }

    private ScholarshipRecommendationResponse toResponse(
            StudentRecommendation rec,
            ScholarshipSummaryClientDto scholarship,
            LocalDate today
    ) {
        LocalDate deadline = (scholarship != null) ? scholarship.applicationDeadline() : null;
        long daysUntil = (deadline != null) ? ChronoUnit.DAYS.between(today, deadline) : -1;

        return new ScholarshipRecommendationResponse(
                rec.getId(),
                rec.getScholarshipId(),
                scholarship != null ? scholarship.title() : "Scholarship #" + rec.getScholarshipId(),
                scholarship != null ? scholarship.slug() : "scholarship-" + rec.getScholarshipId(),
                scholarship != null ? scholarship.providerName() : "Unknown Provider",
                scholarship != null ? scholarship.awardAmountMin() : null,
                scholarship != null ? scholarship.awardAmountMax() : null,
                scholarship != null ? scholarship.currencyCode() : "USD",
                deadline,
                daysUntil,
                rec.getEligibilityVerdict(),
                rec.getEligibilityScore(),
                rec.getMatchScore(),
                rec.getRankPosition(),
                rec.getScoreBreakdown(),
                rec.isDismissed(),
                rec.getCalculatedAt()
        );
    }
}
