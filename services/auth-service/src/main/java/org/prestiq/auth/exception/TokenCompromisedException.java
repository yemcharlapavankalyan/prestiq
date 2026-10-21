package org.prestiq.auth.exception;

public class TokenCompromisedException extends RuntimeException {

    public TokenCompromisedException(String message) {
        super(message);
    }
}
