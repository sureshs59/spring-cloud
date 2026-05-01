package com.example.restpatterns.client;

import com.example.restpatterns.dto.Dtos.*;
import com.example.restpatterns.exception.ServiceUnavailableException;
import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

public class FeignClients {

    // ════════════════════════════════════════════════════════
    //  Feign Global Config — auth, error decoder, log level
    // ════════════════════════════════════════════════════════
    @Configuration
    public static class FeignConfig {

        /** Add auth header to every Feign request automatically */
        @Bean
        public RequestInterceptor authInterceptor() {
            return template -> {
                template.header("Authorization", "Bearer demo-token");
                template.header("X-Source", "rest-patterns-demo");
            };
        }

        /** Map HTTP status codes to custom exceptions */
        @Bean
        public ErrorDecoder errorDecoder() {
            return (methodKey, response) -> switch (response.status()) {
                case 404 -> new RuntimeException("Resource not found in: " + methodKey);
                case 409 -> new RuntimeException("Conflict in: " + methodKey);
                case 503 -> new ServiceUnavailableException("Service down: " + methodKey);
                default  -> new RuntimeException("Error " + response.status() + " in: " + methodKey);
            };
        }

        /** Log headers + body for debugging */
        @Bean
        public Logger.Level feignLoggerLevel() {
            return Logger.Level.BASIC;
        }
    }

    // ════════════════════════════════════════════════════════
    //  Fallbacks — returned when a Feign call fails
    // ════════════════════════════════════════════════════════

    @Component @Slf4j
    public static class UserClientFallback implements UserClient {
        @Override public UserDTO getUserById(Long id) {
            log.warn("[Feign Fallback] getUserById id={}", id);
            return UserDTO.builder().id(id).name("Unknown User").email("N/A").build();
        }
        @Override public List<UserDTO> getAllUsers(int page, int size) { return Collections.emptyList(); }
        @Override public UserDTO createUser(UserDTO user) { throw new ServiceUnavailableException("User service down"); }
        @Override public UserDTO updateUser(Long id, UserDTO user) { throw new ServiceUnavailableException("User service down"); }
        @Override public void deleteUser(Long id) { throw new ServiceUnavailableException("User service down"); }
        @Override public List<UserDTO> searchUsers(String token, String keyword) { return Collections.emptyList(); }
    }

    @Component @Slf4j
    public static class ProductClientFallback implements ProductClient {
        @Override public ProductDTO getProductById(Long id) {
            log.warn("[Feign Fallback] getProductById id={}", id);
            return ProductDTO.builder().id(id).name("Unavailable").price(0.0).build();
        }
        @Override public List<ProductDTO> getAllProducts() { return Collections.emptyList(); }
        @Override public List<ProductDTO> getBatch(String ids) { return Collections.emptyList(); }
    }

    @Component @Slf4j
    public static class OrderClientFallback implements OrderClient {
        @Override public List<OrderDTO> getOrdersByUserId(Long userId) { return Collections.emptyList(); }
        @Override public OrderDTO createOrder(CreateOrderRequest request) { throw new ServiceUnavailableException("Order service down"); }
        @Override public void deleteOrder(Long id) { throw new ServiceUnavailableException("Order service down"); }
    }

    @Component @Slf4j
    public static class PaymentClientFallback implements PaymentClient {
        @Override public PaymentDTO getLatestPayment(Long userId) {
            return PaymentDTO.builder().status("UNAVAILABLE").build();
        }
    }
}