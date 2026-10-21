package org.prestiq.scholarship.exception;

public class IllegalLifecycleTransitionException extends RuntimeException {
    public IllegalLifecycleTransitionException(String message) {
        super(message);
    }
}
