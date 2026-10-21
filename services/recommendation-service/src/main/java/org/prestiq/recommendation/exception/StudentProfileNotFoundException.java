package org.prestiq.recommendation.exception;

public class StudentProfileNotFoundException extends RuntimeException {
    public StudentProfileNotFoundException(String message) {
        super(message);
    }
}
