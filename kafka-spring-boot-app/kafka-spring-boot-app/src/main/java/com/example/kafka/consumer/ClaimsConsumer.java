package com.example.kafka.consumer;

import com.example.kafka.model.ClaimEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ClaimsConsumer {

    /**
     * Listen to claims-events topic
     * This will be called for each message automatically
     */
    @KafkaListener(
        topics = "${kafka.topic.claims}",
        groupId = "claims-processor-group",
        concurrency = "3"  // 3 parallel consumers
    )
    public void consumeClaim(
        @Payload ClaimEvent claimEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset) {
        
        log.info("Received claim from partition {} offset {}: {}",
            partition, offset, claimEvent.getClaimId());
        log.info("Claim details - Member: {}, Amount: {}, Status: {}",
            claimEvent.getMemberId(),
            claimEvent.getAmount(),
            claimEvent.getStatus());

        try {
            // Process claim
            processClaimEvent(claimEvent);
            
            log.info("Successfully processed claim: {}", claimEvent.getClaimId());
            
        } catch (Exception e) {
            log.error("Error processing claim {}: {}",
                claimEvent.getClaimId(), e.getMessage(), e);
            // In production, send to dead letter queue
        }
    }

    /**
     * Alternative listener with manual acknowledgment
     */
    @KafkaListener(
        topics = "${kafka.topic.claims}",
        groupId = "claims-manual-ack-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeClaimWithAck(
        @Payload ClaimEvent claimEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment) {
        
        log.info("Processing claim with manual ack: {}", claimEvent.getClaimId());
        
        try {
            // Process claim
            processClaimEvent(claimEvent);
            
            // Commit offset manually after successful processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
                log.info("Offset committed for claim: {}", claimEvent.getClaimId());
            }
            
        } catch (Exception e) {
            log.error("Failed to process claim: {}", e.getMessage());
            // Don't acknowledge - will retry
        }
    }

    /**
     * Business logic to process claim
     */
    private void processClaimEvent(ClaimEvent claimEvent) {
        // Simulate processing
        log.debug("Processing claim: {}", claimEvent);
        
        // Example validations
        if (claimEvent.getAmount() <= 0) {
            throw new IllegalArgumentException("Invalid amount: " + claimEvent.getAmount());
        }
        
        // Simulate some processing delay
        try {
            Thread.sleep(100); // Simulate work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        
        log.info("Claim {} processed successfully", claimEvent.getClaimId());
    }
}