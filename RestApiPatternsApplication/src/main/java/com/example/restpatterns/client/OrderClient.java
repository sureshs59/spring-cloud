package com.example.restpatterns.client;

import com.example.restpatterns.dto.Dtos;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "order-service", url = "${api.order-service}",
        fallback = FeignClients.OrderClientFallback.class)
public interface OrderClient {
    @GetMapping("/api/orders")
    List<Dtos.OrderDTO> getOrdersByUserId(@RequestParam Long userId);

    @PostMapping("/api/orders")
    Dtos.OrderDTO createOrder(@RequestBody Dtos.CreateOrderRequest request);

    @DeleteMapping("/api/orders/{id}")
    void deleteOrder(@PathVariable Long id);
}
