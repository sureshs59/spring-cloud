package com.example.mulitpleapi.dto;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class OrderDTO {
    private Long   id;
    private Long   userId;
    private Long   productId;
    private Double totalAmount;
    private String status;
}
