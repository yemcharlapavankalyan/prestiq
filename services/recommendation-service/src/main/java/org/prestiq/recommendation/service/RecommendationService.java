package org.prestiq.recommendation.service;

import org.prestiq.recommendation.dto.request.UpdatePreferencesRequest;
import org.prestiq.recommendation.dto.response.PagedResponse;
import org.prestiq.recommendation.dto.response.ScholarshipRecommendationResponse;
import org.prestiq.recommendation.dto.response.StudentPreferenceResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface RecommendationService {

    PagedResponse<ScholarshipRecommendationResponse> getRecommendations(
            UUID studentId,
            int page,
            int size,
            BigDecimal minScore,
            Boolean includeNeedsVerification,
            boolean recalculate
    );

    void dismissRecommendation(UUID studentId, Long scholarshipId);

    void undismissRecommendation(UUID studentId, Long scholarshipId);

    void recalculateRecommendations(UUID studentId);

    StudentPreferenceResponse getPreferences(UUID studentId);

    StudentPreferenceResponse updatePreferences(UUID studentId, UpdatePreferencesRequest request);
}
