package org.prestiq.eligibility.repository;

import org.prestiq.eligibility.domain.entity.EligibilityEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EligibilityEvaluationRepository extends JpaRepository<EligibilityEvaluation, UUID> {

    Optional<EligibilityEvaluation> findByIdAndStudentId(UUID id, UUID studentId);

    List<EligibilityEvaluation> findByStudentIdOrderByEvaluatedAtDesc(UUID studentId);

    Optional<EligibilityEvaluation> findFirstByStudentIdAndScholarshipIdOrderByEvaluatedAtDesc(UUID studentId, Long scholarshipId);
}
