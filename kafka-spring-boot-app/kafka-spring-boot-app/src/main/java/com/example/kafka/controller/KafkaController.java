package com.example.kafka.controller;


import com.example.kafka.model.ClaimEvent;
import com.example.kafka.producer.ClaimsProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class KafkaController {

    private final ClaimsProducer claimsProducer;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Kafka application is running");
        return ResponseEntity.ok(response);
    }

    /**
     * Send a single claim event
     */
    @PostMapping("/send-claim")
    public ResponseEntity<Map<String, String>> sendClaim(
        @RequestBody(required = false) ClaimEvent claimEvent) {
        
        try {
            // Use provided claim or create dummy
            if (claimEvent == null) {
                claimEvent = ClaimEvent.createDummy(1);
            }
            
            claimsProducer.sendClaim(claimEvent);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Claim sent to Kafka");
            response.put("claimId", claimEvent.getClaimId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error sending claim", e);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Send multiple dummy claim events
     * Example: POST /api/kafka/send-claims?count=10
     */
    @PostMapping("/send-claims")
    public ResponseEntity<Map<String, Object>> sendMultipleClaims(
        @RequestParam(defaultValue = "10") int count) {
        
        try {
            log.info("Sending {} claims", count);
            
            claimsProducer.sendMultipleClaims(count);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", count + " claims sent to Kafka");
            response.put("count", count);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error sending claims", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get application info
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> info() {
        Map<String, String> response = new HashMap<>();
        response.put("application", "Kafka Spring Boot Claims Processor");
        response.put("version", "1.0.0");
        response.put("description", "Real-time claims processing with Kafka");
        response.put("endpoints", "POST /api/kafka/send-claim, POST /api/kafka/send-claims?count=10");
        return ResponseEntity.ok(response);
    }
}