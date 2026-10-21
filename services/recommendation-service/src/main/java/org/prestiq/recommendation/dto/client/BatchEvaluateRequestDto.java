package org.prestiq.recommendation.dto.client;

import java.util.List;

public record BatchEvaluateRequestDto(
        StudentProfileClientDto studentProfile,
        List<Long> scholarshipIds
) {
}
