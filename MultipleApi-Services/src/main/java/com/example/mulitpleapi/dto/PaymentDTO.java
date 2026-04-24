package com.example.mulitpleapi.dto;

import lombok.*;

@Getter
@Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class PaymentDTO {
    private Long   id;
    private Long   orderId;
    private Double amount;
    private String status;
}
