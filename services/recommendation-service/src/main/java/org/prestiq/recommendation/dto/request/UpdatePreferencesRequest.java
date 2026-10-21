package org.prestiq.recommendation.dto.request;

import java.math.BigDecimal;
import java.util.List;

public record UpdatePreferencesRequest(
        List<String> preferredFields,
        List<String> preferredLocations,
        BigDecimal minAwardPreference
) {
}
