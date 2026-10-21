package org.prestiq.eligibility.exception;

public class RuleSetNotFoundException extends RuntimeException {
    public RuleSetNotFoundException(String message) {
        super(message);
    }
}
