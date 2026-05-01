package com.example.restpatterns.service.sequential;

import com.example.restpatterns.dto.Dtos.*;
import com.example.restpatterns.exception.ResourceNotFoundException;
import com.example.restpatterns.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * ══════════════════════════════════════════════════════════
 *  PATTERN 1 — Sequential REST calls using RestTemplate
 * ══════════════════════════════════════════════════════════
 *
 * How it works:
 *   Each API call executes ONE AFTER ANOTHER.
 *   Thread blocks waiting for each response before proceeding.
 *
 * Total time = sum of all individual call times
 *   e.g. 300ms + 200ms + 150ms + 100ms = 750ms
 *
 * When to use:
 *   ✅ Call B DEPENDS on the result of Call A
 *   ✅ Simple scenarios, low traffic
 *   ✅ When order of execution matters
 *   ❌ DO NOT use for independent parallel-safe calls
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SequentialApiService {

    private final RestTemplate restTemplate;

    @Value("${api.user-service}")    private String userServiceUrl;
    @Value("${api.product-service}") private String productServiceUrl;
    @Value("${api.order-service}")   private String orderServiceUrl;
    @Value("${api.payment-service}") private String paymentServiceUrl;

    // ────────────────────────────────────────────────────────
    //  Main method — calls 4 services sequentially
    // ────────────────────────────────────────────────────────

    public DashboardDTO getDashboardSequential(Long userId) {
        long start = System.currentTimeMillis();
        log.info("[PATTERN-1 Sequential] Starting 4 sequential calls for userId={}", userId);

        // Call 1 → Call 2 → Call 3 → Call 4 (each waits for previous)
        UserDTO          user     = getUser(userId);           // ~300ms
        List<OrderDTO>   orders   = getOrdersByUser(userId);   // ~200ms
        List<ProductDTO> products = getAllProducts();           // ~150ms
        PaymentDTO       payment  = getLatestPayment(userId);  // ~100ms

        long duration = System.currentTimeMillis() - start;
        log.info("[PATTERN-1 Sequential] Completed in {}ms (sum of all calls)", duration);

        return DashboardDTO.builder()
                .user(user)
                .orders(orders)
                .products(products)
                .payment(payment)
                .pattern("Pattern 1 — Sequential RestTemplate")
                .callDurationMs(duration)
                .build();
    }

    // ────────────────────────────────────────────────────────
    //  Individual API call methods (GET, POST examples)
    // ────────────────────────────────────────────────────────

    /**
     * GET single object.
     * Throws ResourceNotFoundException on 404.
     * Throws ServiceUnavailableException on 5xx.
     */
    public UserDTO getUser(Long id) {
        try {
            ResponseEntity<UserDTO> response = restTemplate.getForEntity(
                    userServiceUrl + "/api/users/{id}",
                    UserDTO.class,
                    id
            );
            return response.getBody();

        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("User", id);
        } catch (HttpServerErrorException ex) {
            log.error("[Sequential] User service 5xx: {}", ex.getMessage());
            throw new ServiceUnavailableException("User service unavailable");
        }
    }

    /**
     * GET list using ParameterizedTypeReference.
     * Required when deserializing generic types (List<T>).
     * Falls back to empty list on error.
     */
    public List<OrderDTO> getOrdersByUser(Long userId) {
        try {
            ResponseEntity<List<OrderDTO>> response = restTemplate.exchange(
                    orderServiceUrl + "/api/orders?userId={id}",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<OrderDTO>>() {},
                    userId
            );
            return response.getBody() != null
                    ? response.getBody()
                    : Collections.emptyList();

        } catch (Exception ex) {
            log.warn("[Sequential] Orders call failed, returning empty: {}", ex.getMessage());
            return Collections.emptyList();   // graceful degradation
        }
    }

    /**
     * GET list — no path variables.
     */
    public List<ProductDTO> getAllProducts() {
        ResponseEntity<List<ProductDTO>> response = restTemplate.exchange(
                productServiceUrl + "/api/products",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ProductDTO>>() {}
        );
        return response.getBody() != null
                ? response.getBody()
                : Collections.emptyList();
    }

    /**
     * GET with query param.
     */
    public PaymentDTO getLatestPayment(Long userId) {
        try {
            return restTemplate.getForObject(
                    paymentServiceUrl + "/api/payments/latest?userId={id}",
                    PaymentDTO.class,
                    userId
            );
        } catch (Exception ex) {
            log.warn("[Sequential] Payment call failed: {}", ex.getMessage());
            return PaymentDTO.builder().status("UNAVAILABLE").build();
        }
    }

    /**
     * POST with request body and custom headers.
     * Shows how to add Authorization header.
     */
    public OrderDTO createOrder(CreateOrderRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer demo-token");
        headers.set("X-Request-Id", java.util.UUID.randomUUID().toString());

        HttpEntity<CreateOrderRequest> entity =
                new HttpEntity<>(request, headers);

        ResponseEntity<OrderDTO> response = restTemplate.postForEntity(
                orderServiceUrl + "/api/orders",
                entity,
                OrderDTO.class
        );
        log.info("[Sequential] Order created: {}", response.getBody());
        return response.getBody();
    }

    /**
     * PUT — update a resource.
     */
    public UserDTO updateUser(Long id, UserDTO updates) {
        HttpEntity<UserDTO> entity = new HttpEntity<>(updates);
        ResponseEntity<UserDTO> response = restTemplate.exchange(
                userServiceUrl + "/api/users/{id}",
                HttpMethod.PUT,
                entity,
                UserDTO.class,
                id
        );
        return response.getBody();
    }

    /**
     * DELETE.
     */
    public void deleteOrder(Long id) {
        restTemplate.delete(
                orderServiceUrl + "/api/orders/{id}", id);
        log.info("[Sequential] Order {} deleted", id);
    }
}