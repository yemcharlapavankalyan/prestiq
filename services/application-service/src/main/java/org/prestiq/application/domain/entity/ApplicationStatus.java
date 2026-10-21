package org.prestiq.application.domain.entity;

import java.util.Collections;
import java.util.Set;

public enum ApplicationStatus {
    PLANNING,
    IN_PROGRESS,
    READY_TO_SUBMIT,
    SUBMITTED,
    UNDER_REVIEW,
    AWARDED,
    REJECTED,
    WITHDRAWN;

    public boolean isTerminal() {
        return this == AWARDED || this == REJECTED || this == WITHDRAWN;
    }

    public boolean isActive() {
        return !isTerminal();
    }

    public boolean canTransitionTo(ApplicationStatus target) {
        if (target == null || this == target) {
            return false;
        }

        return switch (this) {
            case PLANNING -> target == IN_PROGRESS || target == WITHDRAWN;
            case IN_PROGRESS -> target == PLANNING || target == READY_TO_SUBMIT || target == WITHDRAWN;
            case READY_TO_SUBMIT -> target == IN_PROGRESS || target == SUBMITTED || target == WITHDRAWN;
            case SUBMITTED -> target == UNDER_REVIEW || target == AWARDED || target == REJECTED || target == WITHDRAWN;
            case UNDER_REVIEW -> target == AWARDED || target == REJECTED || target == WITHDRAWN;
            case AWARDED, REJECTED -> false;
            case WITHDRAWN -> target == PLANNING;
        };
    }

    public Set<ApplicationStatus> getAllowedNextStatuses() {
        return switch (this) {
            case PLANNING -> Set.of(IN_PROGRESS, WITHDRAWN);
            case IN_PROGRESS -> Set.of(PLANNING, READY_TO_SUBMIT, WITHDRAWN);
            case READY_TO_SUBMIT -> Set.of(IN_PROGRESS, SUBMITTED, WITHDRAWN);
            case SUBMITTED -> Set.of(UNDER_REVIEW, AWARDED, REJECTED, WITHDRAWN);
            case UNDER_REVIEW -> Set.of(AWARDED, REJECTED, WITHDRAWN);
            case AWARDED, REJECTED -> Collections.emptySet();
            case WITHDRAWN -> Set.of(PLANNING);
        };
    }
}
