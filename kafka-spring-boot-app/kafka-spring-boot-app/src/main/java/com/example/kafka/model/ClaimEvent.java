package com.example.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimEvent {
    
    @JsonProperty("claim_id")
    private String claimId;
    
    @JsonProperty("member_id")
    private String memberId;
    
    @JsonProperty("amount")
    private double amount;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("event_type")
    private String eventType;
    
    @JsonProperty("description")
    private String description;
    
    /**
     * Create a dummy claim event
     */
    public static ClaimEvent createDummy(int index) {
        return ClaimEvent.builder()
            .claimId("CLAIM-" + System.currentTimeMillis() + "-" + index)
            .memberId("MEMBER-" + (index % 10))
            .amount(100.00 + (index * 10))
            .status("SUBMITTED")
            .timestamp(System.currentTimeMillis())
            .eventType("CLAIM_SUBMITTED")
            .description("Claim submitted for member " + (index % 10))
            .build();
    }
}