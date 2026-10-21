package org.prestiq.eligibility.dto.response;

import java.util.List;

public record BatchEvaluateResponse(
        List<BatchEvaluateItemResponse> evaluations
) {
}
