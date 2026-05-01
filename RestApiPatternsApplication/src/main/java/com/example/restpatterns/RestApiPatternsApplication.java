package com.example.restpatterns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * REST API Patterns Demo Application.
 *
 * Demonstrates 6 ways to handle multiple REST API calls
 * in Spring Boot. Run this class, then call the endpoints
 * shown in README.md or use the curl commands below.
 *
 * Quick start:
 *   mvn spring-boot:run
 *   curl http://localhost:8080/api/v1/dashboard/sequential/1
 */
@SpringBootApplication
@EnableFeignClients
@EnableAsync
public class RestApiPatternsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestApiPatternsApplication.class, args);
    }
}