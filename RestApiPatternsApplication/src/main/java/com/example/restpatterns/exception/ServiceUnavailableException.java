package com.example.restpatterns.exception;

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
    public ServiceUnavailableException(String service, Throwable cause) {
        super("Service unavailable: " + service, cause);
    }
}