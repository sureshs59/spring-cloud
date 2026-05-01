package com.example.restpatterns.service.resilient;

import com.example.restpatterns.dto.Dtos.*;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * PATTERN 5 — Retry + Circuit Breaker (Resilience4j)
 * Protects against failures, retries transient errors,
 * opens circuit on repeated failures to prevent cascade.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResilientApiService {

    private final RestTemplate restTemplate;
    private final Executor     apiExecutor;

    @Value("${api.user-service}")    private String userServiceUrl;
    @Value("${api.product-service}") private String productServiceUrl;
    @Value("${api.order-service}")   private String orderServiceUrl;
    @Value("${api.payment-service}") private String paymentServiceUrl;

    // ────────────────────────────────────────────────────────
    //  @Retry — retries on failure (3 attempts, exp backoff)
    // ────────────────────────────────────────────────────────
    @Retry(name = "userService", fallbackMethod = "getUserFallback")
    public UserDTO getUser(Long id) {
        log.info("[PATTERN-5 Retry] Attempt to fetch user id={}", id);
        return restTemplate.getForObject(
                userServiceUrl + "/api/users/{id}", UserDTO.class, id);
    }

    // ────────────────────────────────────────────────────────
    //  @CircuitBreaker — opens after 50% failure rate
    // ────────────────────────────────────────────────────────
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
    public UserDTO getUserWithCircuitBreaker(Long id) {
        log.info("[PATTERN-5 CB] Fetching user id={}", id);
        return restTemplate.getForObject(
                userServiceUrl + "/api/users/{id}", UserDTO.class, id);
    }

    // ────────────────────────────────────────────────────────
    //  Retry + CircuitBreaker combined (RECOMMENDED)
    //  Order: Retry runs INSIDE CircuitBreaker
    //  If all retries fail -> CB records one failure
    // ────────────────────────────────────────────────────────
    @Retry(name = "userService")
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
    public UserDTO getUserResilient(Long id) {
        log.info("[PATTERN-5 Retry+CB] Fetching user id={}", id);
        return restTemplate.getForObject(
                userServiceUrl + "/api/users/{id}", UserDTO.class, id);
    }

    // ────────────────────────────────────────────────────────
    //  @RateLimiter — max 20 calls per second
    // ────────────────────────────────────────────────────────
    @RateLimiter(name = "userService", fallbackMethod = "getUserFallback")
    public UserDTO getUserRateLimited(Long id) {
        log.info("[PATTERN-5 RateLimit] Fetching user id={}", id);
        return restTemplate.getForObject(
                userServiceUrl + "/api/users/{id}", UserDTO.class, id);
    }

    // ────────────────────────────────────────────────────────
    //  Full resilient dashboard — all services protected
    // ────────────────────────────────────────────────────────
    public DashboardDTO getDashboard(Long userId) {
        long start = System.currentTimeMillis();
        log.info("[PATTERN-5] Resilient dashboard for userId={}", userId);

        // Each call is independently protected
        CompletableFuture<UserDTO>    uF = CompletableFuture.supplyAsync(() -> getUserResilient(userId),           apiExecutor);
        CompletableFuture<List>       oF = CompletableFuture.supplyAsync(() -> getOrdersResilient(userId),         apiExecutor);
        CompletableFuture<List>       pF = CompletableFuture.supplyAsync(() -> getProductsResilient(),             apiExecutor);
        CompletableFuture<PaymentDTO> pyF= CompletableFuture.supplyAsync(() -> getPaymentResilient(userId),        apiExecutor);

        CompletableFuture.allOf(uF, oF, pF, pyF).join();

        return DashboardDTO.builder()
                .user(uF.join())
                .pattern("Pattern 5 — Resilience4j Retry+CircuitBreaker")
                .callDurationMs(System.currentTimeMillis() - start).build();
    }

    @Retry(name = "productService")
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductsFallback")
    public List<ProductDTO> getProductsResilient() {
        var r = restTemplate.exchange(productServiceUrl + "/api/products",
                org.springframework.http.HttpMethod.GET, null,
                new org.springframework.core.ParameterizedTypeReference<List<ProductDTO>>() {});
        return r.getBody() != null ? r.getBody() : List.of();
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getOrdersFallback")
    public List<OrderDTO> getOrdersResilient(Long userId) {
        var r = restTemplate.exchange(orderServiceUrl + "/api/orders?userId={id}",
                org.springframework.http.HttpMethod.GET, null,
                new org.springframework.core.ParameterizedTypeReference<List<OrderDTO>>() {}, userId);
        return r.getBody() != null ? r.getBody() : List.of();
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getPaymentFallback")
    public PaymentDTO getPaymentResilient(Long userId) {
        return restTemplate.getForObject(
                paymentServiceUrl + "/api/payments/latest?userId={id}", PaymentDTO.class, userId);
    }

    // ────────────────────────────────────────────────────────
    //  Fallback methods — MUST match original signature + Throwable
    // ────────────────────────────────────────────────────────
    public UserDTO getUserFallback(Long id, Throwable ex) {
        log.warn("[PATTERN-5 Fallback] getUserFallback id={} reason={}", id, ex.getMessage());
        return UserDTO.builder().id(id).name("Service Unavailable").email("N/A").build();
    }

    public UserDTO getUserFallback(Long id, CallNotPermittedException ex) {
        log.error("[PATTERN-5 Fallback] Circuit OPEN for user id={}", id);
        return UserDTO.builder().id(id).name("Circuit Open — Try Later").build();
    }

    public List<ProductDTO> getProductsFallback(Throwable ex) {
        log.warn("[PATTERN-5 Fallback] Products unavailable: {}", ex.getMessage());
        return List.of();
    }

    public List<OrderDTO> getOrdersFallback(Long userId, Throwable ex) {
        log.warn("[PATTERN-5 Fallback] Orders unavailable for userId={}", userId);
        return List.of();
    }

    public PaymentDTO getPaymentFallback(Long userId, Throwable ex) {
        log.warn("[PATTERN-5 Fallback] Payment unavailable for userId={}", userId);
        return PaymentDTO.builder().status("UNAVAILABLE").build();
    }
}