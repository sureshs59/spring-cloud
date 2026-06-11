package com.example.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import com.example.kafka.model.ClaimEvent;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimsProducer {

    private final KafkaTemplate<String, ClaimEvent> kafkaTemplate;

    @Value("${kafka.topic.claims}")
    private String claimsTopic;

    /**
     * Send claim event to Kafka
     */
    public void sendClaim(ClaimEvent claimEvent) {
        log.info("Publishing claim event: {}", claimEvent.getClaimId());
        
        try {
            // Build message with headers
            Message<ClaimEvent> message = MessageBuilder
                .withPayload(claimEvent)
                .setHeader(KafkaHeaders.TOPIC, claimsTopic)
                .setHeader(KafkaHeaders.KEY, claimEvent.getClaimId())
                .setHeader("correlation-id", claimEvent.getClaimId())
                .setHeader("timestamp", System.currentTimeMillis())
                .build();

            // Send asynchronously
            kafkaTemplate.send(message)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Claim {} sent successfully to partition {} offset {}",
                            claimEvent.getClaimId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send claim {}: {}", 
                            claimEvent.getClaimId(), ex.getMessage(), ex);
                    }
                });

        } catch (Exception e) {
            log.error("Error publishing claim: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish claim", e);
        }
    }

    /**
     * Send multiple claims
     */
    public void sendMultipleClaims(int count) {
        log.info("Publishing {} claim events", count);
        
        for (int i = 0; i < count; i++) {
            ClaimEvent claim = ClaimEvent.createDummy(i);
            sendClaim(claim);
            
            // Small delay between sends (optional)
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread interrupted", e);
            }
        }
    }

    /**
     * Send claim synchronously (for testing)
     */
    public void sendClaimSync(ClaimEvent claimEvent) throws Exception {
        log.info("Sending claim synchronously: {}", claimEvent.getClaimId());
        
        kafkaTemplate.send(claimsTopic, claimEvent.getClaimId(), claimEvent)
            .get(); // Blocking call
        
        log.info("Claim sent synchronously");
    }
}