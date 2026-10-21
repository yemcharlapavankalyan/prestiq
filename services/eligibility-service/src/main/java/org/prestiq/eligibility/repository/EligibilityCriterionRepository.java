package org.prestiq.eligibility.repository;

import org.prestiq.eligibility.domain.entity.EligibilityCriterion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EligibilityCriterionRepository extends JpaRepository<EligibilityCriterion, Long> {

    List<EligibilityCriterion> findByRuleSetId(Long ruleSetId);
}
