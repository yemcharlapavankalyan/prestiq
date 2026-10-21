package org.prestiq.recommendation.repository;

import org.prestiq.recommendation.domain.entity.EligibilityVerdict;
import org.prestiq.recommendation.domain.entity.StudentRecommendation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRecommendationRepository extends JpaRepository<StudentRecommendation, UUID> {

    Optional<StudentRecommendation> findByStudentIdAndScholarshipId(UUID studentId, Long scholarshipId);

    List<StudentRecommendation> findByStudentIdAndIsDismissedFalseOrderByRankPositionAsc(UUID studentId);

    Page<StudentRecommendation> findByStudentIdAndIsDismissedFalse(UUID studentId, Pageable pageable);

    Page<StudentRecommendation> findByStudentIdAndIsDismissedFalseAndMatchScoreGreaterThanEqual(
            UUID studentId,
            BigDecimal minScore,
            Pageable pageable
    );

    Page<StudentRecommendation> findByStudentIdAndIsDismissedFalseAndEligibilityVerdictInAndMatchScoreGreaterThanEqual(
            UUID studentId,
            Collection<EligibilityVerdict> verdicts,
            BigDecimal minScore,
            Pageable pageable
    );

    long countByStudentId(UUID studentId);

    List<StudentRecommendation> findByStudentId(UUID studentId);

    void deleteByStudentId(UUID studentId);
}
