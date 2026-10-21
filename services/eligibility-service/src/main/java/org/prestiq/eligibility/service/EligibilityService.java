package org.prestiq.eligibility.service;

import org.prestiq.eligibility.dto.request.BatchEvaluateRequest;
import org.prestiq.eligibility.dto.response.BatchEvaluateResponse;
import org.prestiq.eligibility.dto.response.EligibilityEvaluationResponse;
import org.prestiq.eligibility.dto.response.StudentProfileDto;
import org.prestiq.eligibility.security.UserPrincipal;

import java.util.List;
import java.util.UUID;

public interface EligibilityService {

    EligibilityEvaluationResponse evaluateStudent(
            UUID studentId,
            Long scholarshipId,
            StudentProfileDto profileOverride
    );

    EligibilityEvaluationResponse getEvaluationById(UUID evaluationId, UserPrincipal principal);

    List<EligibilityEvaluationResponse> getMyEvaluations(UUID studentId);

    BatchEvaluateResponse batchEvaluate(BatchEvaluateRequest request);
}
