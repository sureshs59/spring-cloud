package com.example.restpatterns.exception;

import com.example.restpatterns.dto.Dtos;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.TimeoutException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Dtos.ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex){
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(404).body(Dtos.ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Dtos.ApiResponse<Void>> handleServiceDown(ServiceUnavailableException ex){
        log.error("Service unavailable: {}", ex.getMessage());
        return ResponseEntity.status(503).body(Dtos.ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<Dtos.ApiResponse<Void>> handleCircuitOpen(CallNotPermittedException ex){
        log.error("Circuit open: {}", ex.getMessage());
        return ResponseEntity.status(503).body(Dtos.ApiResponse.error("Service temporarily unavailable - Circuit breaker open"));
    }
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<Dtos.ApiResponse<Void>> handleTimeout(
            TimeoutException ex) {
        log.error("Request timed out: {}", ex.getMessage());
        return ResponseEntity.status(504)
                .body(Dtos.ApiResponse.error("Request timed out"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Dtos.ApiResponse<Void>> handleAll(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity.status(500)
                .body(Dtos.ApiResponse.error("Internal server error"));
    }
}
