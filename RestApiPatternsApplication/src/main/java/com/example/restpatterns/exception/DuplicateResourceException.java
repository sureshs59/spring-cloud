package com.example.restpatterns.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String service, Throwable cause) {
        super("Service unavailable: " + service, cause);
    }
}