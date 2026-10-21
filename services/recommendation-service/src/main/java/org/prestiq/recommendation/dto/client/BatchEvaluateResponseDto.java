package org.prestiq.recommendation.dto.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BatchEvaluateResponseDto(
        List<BatchEvaluateItemDto> evaluations
) {
}
