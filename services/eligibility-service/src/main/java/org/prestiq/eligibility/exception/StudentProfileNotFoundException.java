package org.prestiq.eligibility.exception;

public class StudentProfileNotFoundException extends RuntimeException {
    public StudentProfileNotFoundException(String message) {
        super(message);
    }
}
