package org.prestiq.application.exception;

import org.prestiq.application.domain.entity.ApplicationStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    private final ApplicationStatus fromStatus;
    private final ApplicationStatus toStatus;

    public InvalidStatusTransitionException(ApplicationStatus fromStatus, ApplicationStatus toStatus) {
        super(String.format("Invalid application status transition from %s to %s. Allowed transitions from %s are: %s",
                fromStatus, toStatus, fromStatus, fromStatus.getAllowedNextStatuses()));
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public ApplicationStatus getFromStatus() {
        return fromStatus;
    }

    public ApplicationStatus getToStatus() {
        return toStatus;
    }
}
