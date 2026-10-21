package org.prestiq.eligibility.repository;

import org.prestiq.eligibility.domain.entity.RuleSetStatus;
import org.prestiq.eligibility.domain.entity.ScholarshipRuleSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScholarshipRuleSetRepository extends JpaRepository<ScholarshipRuleSet, Long> {

    Optional<ScholarshipRuleSet> findByScholarshipIdAndStatus(Long scholarshipId, RuleSetStatus status);

    Optional<ScholarshipRuleSet> findByScholarshipIdAndVersion(Long scholarshipId, int version);

    List<ScholarshipRuleSet> findByScholarshipIdOrderByVersionDesc(Long scholarshipId);

    Optional<ScholarshipRuleSet> findFirstByScholarshipIdOrderByVersionDesc(Long scholarshipId);

    @Query("SELECT r FROM ScholarshipRuleSet r LEFT JOIN FETCH r.criteria WHERE r.scholarshipId = :scholarshipId AND r.status = :status")
    Optional<ScholarshipRuleSet> findActiveWithCriteria(@Param("scholarshipId") Long scholarshipId, @Param("status") RuleSetStatus status);

    @Query("SELECT r FROM ScholarshipRuleSet r LEFT JOIN FETCH r.criteria WHERE r.scholarshipId IN :scholarshipIds AND r.status = 'ACTIVE'")
    List<ScholarshipRuleSet> findActiveWithCriteriaForScholarships(@Param("scholarshipIds") List<Long> scholarshipIds);

    boolean existsByScholarshipIdAndVersion(Long scholarshipId, int version);
}
