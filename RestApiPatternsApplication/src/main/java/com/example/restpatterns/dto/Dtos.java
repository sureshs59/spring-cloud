package com.example.restpatterns.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// ════════════════════════════════════════════════════════════
//  DTOs — all in one file for easy navigation
// ════════════════════════════════════════════════════════════

public class Dtos {

    // ── UserDTO ───────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserDTO {
        private Long   id;
        private String name;
        private String email;
        private String department;
        private String phone;
    }

    // ── ProductDTO ────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductDTO {
        private Long    id;
        private String  name;
        private Double  price;
        private Integer stock;
        private String  category;
    }

    // ── OrderDTO ──────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderDTO {
        private Long   id;
        private Long   userId;
        private Long   productId;
        private Double totalAmount;
        private String status;
    }

    // ── PaymentDTO ────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentDTO {
        private Long   id;
        private Long   orderId;
        private Double amount;
        private String status;
        private String method;
    }

    // ── DashboardDTO — aggregated result ─────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DashboardDTO {
        private UserDTO          user;
        private List<ProductDTO> products;
        private List<OrderDTO>   orders;
        private PaymentDTO       payment;
        private String           pattern;       // which pattern was used
        private long             callDurationMs;
    }

    // ── ApiResponse — standard wrapper ───────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiResponse<T> {
        private boolean       success;
        private String        message;
        private T             data;
        private LocalDateTime timestamp;

        public static <T> ApiResponse<T> ok(T data, String message) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message(message)
                    .data(data)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        public static <T> ApiResponse<T> ok(T data) {
            return ok(data, "Success");
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    // ── CreateOrderRequest ────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateOrderRequest {
        private Long   userId;
        private Long   productId;
        private Double amount;
        private String notes;
    }
}