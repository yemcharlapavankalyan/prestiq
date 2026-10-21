package org.prestiq.scholarship.exception;

public class ScholarshipNotFoundException extends RuntimeException {
    public ScholarshipNotFoundException(String message) {
        super(message);
    }
}
