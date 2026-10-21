package org.prestiq.application.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApplicationStatus Lifecycle & State Machine Unit Tests")
class ApplicationStatusLifecycleUnitTest {

    @Test
    @DisplayName("Active vs Terminal status classification")
    void testActiveAndTerminalClassification() {
        assertTrue(ApplicationStatus.PLANNING.isActive());
        assertTrue(ApplicationStatus.IN_PROGRESS.isActive());
        assertTrue(ApplicationStatus.READY_TO_SUBMIT.isActive());
        assertTrue(ApplicationStatus.SUBMITTED.isActive());
        assertTrue(ApplicationStatus.UNDER_REVIEW.isActive());

        assertFalse(ApplicationStatus.PLANNING.isTerminal());
        assertFalse(ApplicationStatus.IN_PROGRESS.isTerminal());
        assertFalse(ApplicationStatus.READY_TO_SUBMIT.isTerminal());
        assertFalse(ApplicationStatus.SUBMITTED.isTerminal());
        assertFalse(ApplicationStatus.UNDER_REVIEW.isTerminal());

        assertTrue(ApplicationStatus.AWARDED.isTerminal());
        assertTrue(ApplicationStatus.REJECTED.isTerminal());
        assertTrue(ApplicationStatus.WITHDRAWN.isTerminal());

        assertFalse(ApplicationStatus.AWARDED.isActive());
        assertFalse(ApplicationStatus.REJECTED.isActive());
        assertFalse(ApplicationStatus.WITHDRAWN.isActive());
    }

    @Test
    @DisplayName("Valid status transitions follow documented lifecycle")
    void testValidTransitions() {
        // From PLANNING
        assertTrue(ApplicationStatus.PLANNING.canTransitionTo(ApplicationStatus.IN_PROGRESS));
        assertTrue(ApplicationStatus.PLANNING.canTransitionTo(ApplicationStatus.WITHDRAWN));

        // From IN_PROGRESS
        assertTrue(ApplicationStatus.IN_PROGRESS.canTransitionTo(ApplicationStatus.PLANNING));
        assertTrue(ApplicationStatus.IN_PROGRESS.canTransitionTo(ApplicationStatus.READY_TO_SUBMIT));
        assertTrue(ApplicationStatus.IN_PROGRESS.canTransitionTo(ApplicationStatus.WITHDRAWN));

        // From READY_TO_SUBMIT
        assertTrue(ApplicationStatus.READY_TO_SUBMIT.canTransitionTo(ApplicationStatus.IN_PROGRESS));
        assertTrue(ApplicationStatus.READY_TO_SUBMIT.canTransitionTo(ApplicationStatus.SUBMITTED));
        assertTrue(ApplicationStatus.READY_TO_SUBMIT.canTransitionTo(ApplicationStatus.WITHDRAWN));

        // From SUBMITTED
        assertTrue(ApplicationStatus.SUBMITTED.canTransitionTo(ApplicationStatus.UNDER_REVIEW));
        assertTrue(ApplicationStatus.SUBMITTED.canTransitionTo(ApplicationStatus.AWARDED));
        assertTrue(ApplicationStatus.SUBMITTED.canTransitionTo(ApplicationStatus.REJECTED));
        assertTrue(ApplicationStatus.SUBMITTED.canTransitionTo(ApplicationStatus.WITHDRAWN));

        // From UNDER_REVIEW
        assertTrue(ApplicationStatus.UNDER_REVIEW.canTransitionTo(ApplicationStatus.AWARDED));
        assertTrue(ApplicationStatus.UNDER_REVIEW.canTransitionTo(ApplicationStatus.REJECTED));
        assertTrue(ApplicationStatus.UNDER_REVIEW.canTransitionTo(ApplicationStatus.WITHDRAWN));

        // From WITHDRAWN (can re-open into PLANNING)
        assertTrue(ApplicationStatus.WITHDRAWN.canTransitionTo(ApplicationStatus.PLANNING));
    }

    @Test
    @DisplayName("Invalid status transitions are strictly rejected")
    void testInvalidTransitions() {
        // Transition to self is invalid
        assertFalse(ApplicationStatus.PLANNING.canTransitionTo(ApplicationStatus.PLANNING));
        assertFalse(ApplicationStatus.SUBMITTED.canTransitionTo(ApplicationStatus.SUBMITTED));

        // Transition to null is invalid
        assertFalse(ApplicationStatus.PLANNING.canTransitionTo(null));

        // Jumping directly from PLANNING to SUBMITTED or AWARDED is forbidden
        assertFalse(ApplicationStatus.PLANNING.canTransitionTo(ApplicationStatus.SUBMITTED));
        assertFalse(ApplicationStatus.PLANNING.canTransitionTo(ApplicationStatus.AWARDED));
        assertFalse(ApplicationStatus.PLANNING.canTransitionTo(ApplicationStatus.REJECTED));

        // AWARDED and REJECTED are terminal final states
        assertFalse(ApplicationStatus.AWARDED.canTransitionTo(ApplicationStatus.PLANNING));
        assertFalse(ApplicationStatus.AWARDED.canTransitionTo(ApplicationStatus.SUBMITTED));
        assertFalse(ApplicationStatus.REJECTED.canTransitionTo(ApplicationStatus.IN_PROGRESS));
        assertFalse(ApplicationStatus.REJECTED.canTransitionTo(ApplicationStatus.AWARDED));

        // WITHDRAWN cannot jump directly to SUBMITTED without re-planning
        assertFalse(ApplicationStatus.WITHDRAWN.canTransitionTo(ApplicationStatus.SUBMITTED));
        assertFalse(ApplicationStatus.WITHDRAWN.canTransitionTo(ApplicationStatus.AWARDED));
    }

    @Test
    @DisplayName("Allowed next statuses set matches canTransitionTo logic")
    void testAllowedNextStatuses() {
        assertEquals(Set.of(ApplicationStatus.IN_PROGRESS, ApplicationStatus.WITHDRAWN),
                ApplicationStatus.PLANNING.getAllowedNextStatuses());

        assertEquals(Set.of(ApplicationStatus.PLANNING, ApplicationStatus.READY_TO_SUBMIT, ApplicationStatus.WITHDRAWN),
                ApplicationStatus.IN_PROGRESS.getAllowedNextStatuses());

        assertEquals(Set.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.AWARDED, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
                ApplicationStatus.SUBMITTED.getAllowedNextStatuses());

        assertTrue(ApplicationStatus.AWARDED.getAllowedNextStatuses().isEmpty());
        assertTrue(ApplicationStatus.REJECTED.getAllowedNextStatuses().isEmpty());
    }
}
