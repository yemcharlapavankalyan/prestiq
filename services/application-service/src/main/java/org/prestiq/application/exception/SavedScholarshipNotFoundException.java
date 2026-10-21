package org.prestiq.application.exception;

public class SavedScholarshipNotFoundException extends RuntimeException {
    public SavedScholarshipNotFoundException(String message) {
        super(message);
    }
}
