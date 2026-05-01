package com.example.restpatterns.config;

import com.example.restpatterns.exception.DuplicateResourceException;
import com.example.restpatterns.exception.ResourceNotFoundException;
import com.example.restpatterns.exception.ServiceUnavailableException;
import feign.Logger;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    // Add auth token to EVERY Feign request
    @Bean
    public RequestInterceptor authInterceptor() {
        return requestTemplate -> {
            String token = SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getCredentials()
                    .toString();
            requestTemplate.header("Authorization", "Bearer " + token);
            requestTemplate.header("X-Correlation-ID",
                    MDC.get("correlationId"));   // for distributed tracing
        };
    }

    // Map HTTP error codes to exceptions
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> switch (response.status()) {
            case 404 -> new ResourceNotFoundException(
                    "Not found in: " + methodKey);
            case 409 -> new DuplicateResourceException(
                    "Conflict in: " + methodKey);
            case 503 -> new ServiceUnavailableException(
                    "Service down: " + methodKey);
            default  -> new Exception(
                    "Error " + response.status() + " in: " + methodKey);
        };
    }

    // Retry config
    @Bean
    public Retryer feignRetryer() {
        // retry every 100ms, max 1000ms, max 3 attempts
        return new Retryer.Default(100, 1000, 3);
    }

    // Logger level
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
