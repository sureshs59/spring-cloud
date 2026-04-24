package com.example.mulitpleapi.service;

import com.example.mulitpleapi.dto.DashboardDTO;
import com.example.mulitpleapi.dto.OrderDTO;
import com.example.mulitpleapi.dto.ProductDTO;
import com.example.mulitpleapi.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Collections;
import java.util.List;

/**
 * PATTERN 1 — Sequential Calls (RestTemplate)
 * When to use
 * Simple, low-traffic scenarios. When call B depends on result of call A.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SequentialApiService {

    private final RestTemplate restTemplate;

    @Value("${api.user-service}")    private String userServiceUrl;
    @Value("${api.product-service}") private String productServiceUrl;
    @Value("${api.order-service}")   private String orderServiceUrl;

    /**
     * Sequential calls — each waits for previous to complete.
     *
     * Total time = sum of all call times
     * Use when: Call B needs data from Call A
     *
     * Example: get user → use userId to get orders
     */
    public DashboardDTO getDashboardSequential(Long userId) {
        long start = System.currentTimeMillis();
        log.info("Starting sequential calls for userId={}", userId);

        // Call 1: Get user (300ms)
        UserDTO user = getUserById(userId);

        // Call 2: Get orders using userId (200ms)
        List<OrderDTO> orders = getOrdersByUserId(userId);

        // Call 3: Get products (150ms)
        List<ProductDTO> products = getProducts();

        // Total: ~650ms (300 + 200 + 150)
        long duration = System.currentTimeMillis() - start;
        log.info("Sequential calls completed in {}ms", duration);

        return DashboardDTO.builder()
                .user(user)
                .orders(orders)
                .products(products)
                .callDurationMs(duration)
                .build();
    }

    // ── Individual API call methods ────────────────────

    public UserDTO getUserById(Long id) {
        try {
            ResponseEntity<UserDTO> response = restTemplate.getForEntity(
                    userServiceUrl + "/api/users/{id}",
                    UserDTO.class,
                    id
            );
            return response.getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("User not found: " + id);
        } catch (HttpServerErrorException ex) {
            log.error("User service error: {}", ex.getMessage());
            throw ex;
        }
    }

    public List<OrderDTO> getOrdersByUserId(Long userId) {
        try {
            ResponseEntity<List<OrderDTO>> response = restTemplate.exchange(
                    orderServiceUrl + "/api/orders?userId={userId}",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<OrderDTO>>() {},
                    userId
            );
            return response.getBody() != null
                    ? response.getBody()
                    : Collections.emptyList();
        } catch (Exception ex) {
            log.error("Order service error: {}", ex.getMessage());
            return Collections.emptyList();   // graceful fallback
        }
    }

    public List<ProductDTO> getProducts() {
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

    // POST with request body
    public OrderDTO createOrder(OrderDTO request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + getToken());

        HttpEntity<OrderDTO> entity = new HttpEntity<>(request, headers);

        ResponseEntity<OrderDTO> response = restTemplate.postForEntity(
                orderServiceUrl + "/api/orders",
                entity,
                OrderDTO.class
        );
        return response.getBody();
    }

    private String getToken() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getCredentials()
                .toString();
    }
}
