package org.prestiq.eligibility.engine;

import org.prestiq.eligibility.domain.entity.CriterionOperator;
import org.prestiq.eligibility.domain.entity.EligibilityCriterion;
import org.prestiq.eligibility.exception.InvalidCriterionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EvaluatorRegistry {

    private static final Logger log = LoggerFactory.getLogger(EvaluatorRegistry.class);
    private final List<CriterionEvaluator> evaluators;
    private final StudentFieldResolver fieldResolver;

    public EvaluatorRegistry(List<CriterionEvaluator> evaluators, StudentFieldResolver fieldResolver) {
        this.evaluators = evaluators;
        this.fieldResolver = fieldResolver;
    }

    public CriterionEvaluator findEvaluator(EligibilityCriterion criterion) {
        if (!fieldResolver.isAllowedField(criterion.getTargetField())) {
            throw new InvalidCriterionException("Field '" + criterion.getTargetField() + "' is not in the allowlist of evaluatable attributes.");
        }

        CriterionOperator op = criterion.getOperator();
        String field = criterion.getTargetField();

        for (CriterionEvaluator evaluator : evaluators) {
            if (evaluator.supports(op, field)) {
                return evaluator;
            }
        }

        throw new InvalidCriterionException("No supported evaluator found for operator '" + op + "' on field '" + field + "'");
    }
}
