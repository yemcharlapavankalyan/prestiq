package org.prestiq.application.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SaveScholarshipRequest(
        String notes,
        String scholarshipTitle,
        String scholarshipProvider,
        LocalDate applicationDeadline,
        BigDecimal awardAmountMax
) {
}
