package org.prestiq.eligibility.engine;

import org.prestiq.eligibility.domain.entity.CriterionOperator;
import org.prestiq.eligibility.domain.entity.EligibilityCriterion;
import org.prestiq.eligibility.dto.response.CriterionResultResponse;
import org.prestiq.eligibility.dto.response.StudentProfileDto;

public interface CriterionEvaluator {

    boolean supports(CriterionOperator operator, String targetField);

    CriterionResultResponse evaluate(
            EligibilityCriterion criterion,
            Object studentRawValue,
            StudentProfileDto fullProfile
    );
}
