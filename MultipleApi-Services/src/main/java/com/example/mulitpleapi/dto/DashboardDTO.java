package com.example.mulitpleapi.dto;

import lombok.*;

import java.util.List;

@Getter
@Builder
public class DashboardDTO {
    private UserDTO    user;
    private List<ProductDTO> products;
    private List<OrderDTO>   orders;
    private PaymentDTO payment;
    private long callDurationMs;
}
