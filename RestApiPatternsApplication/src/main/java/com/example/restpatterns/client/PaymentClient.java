package com.example.restpatterns.client;

import com.example.restpatterns.dto.Dtos;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "payment-service", url = "${api.payment-service}",
        fallback = FeignClients.PaymentClientFallback.class)
public interface PaymentClient {
    @GetMapping("/api/payments/latest")
    Dtos.PaymentDTO getLatestPayment(@RequestParam Long userId);
}
