package com.example.mulitpleapi.service;
import com.example.mulitpleapi.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * PATTERN 2 — Parallel Calls (CompletableFuture)
 * When to use
 * Independent calls that don't depend on each other. Biggest performance boost.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class ParallelApiService {

    private final RestTemplate restTemplate;
    private final Executor apiExecutor;

    @Value("${api.user-service}")    private String userServiceUrl;
    @Value("${api.product-service}") private String productServiceUrl;
    @Value("${api.order-service}")   private String orderServiceUrl;
    @Value("${api.payment-service}") private String paymentServiceUrl;

    /**
     * Parallel calls using CompletableFuture.
     *
     * All 4 calls fire SIMULTANEOUSLY.
     * Total time = max(individual times) NOT sum!
     *
     * Sequential: 300 + 200 + 150 + 100 = 750ms
     * Parallel:   max(300, 200, 150, 100) = 300ms  ← 60% faster!
     */
    public DashboardDTO getDashboardParallel(Long userId) {
        long start = System.currentTimeMillis();

        // ── Fire all calls simultaneously ─────────────
        CompletableFuture<UserDTO> userFuture =
                CompletableFuture.supplyAsync(
                        () -> fetchUser(userId), apiExecutor);

        CompletableFuture<List<OrderDTO>> ordersFuture =
                CompletableFuture.supplyAsync(
                        () -> fetchOrders(userId), apiExecutor);

        CompletableFuture<List<ProductDTO>> productsFuture =
                CompletableFuture.supplyAsync(
                        () -> fetchProducts(), apiExecutor);

        CompletableFuture<PaymentDTO> paymentFuture =
                CompletableFuture.supplyAsync(
                        () -> fetchLatestPayment(userId), apiExecutor);

        // ── Wait for ALL to complete ───────────────────
        CompletableFuture.allOf(
                userFuture, ordersFuture,
                productsFuture, paymentFuture
        ).join();  // blocks until all done

        long duration = System.currentTimeMillis() - start;
        log.info("Parallel calls completed in {}ms", duration);

        return DashboardDTO.builder()
                .user(userFuture.join())
                .orders(ordersFuture.join())
                .products(productsFuture.join())
                .payment(paymentFuture.join())
                .callDurationMs(duration)
                .build();
    }

    /**
     * Parallel with TIMEOUT — don't wait forever!
     * Each call has its own timeout.
     */
    public DashboardDTO getDashboardWithTimeout(Long userId) {
        long start = System.currentTimeMillis();

        CompletableFuture<UserDTO> userFuture =
                CompletableFuture.supplyAsync(
                                () -> fetchUser(userId), apiExecutor)
                        .orTimeout(5, TimeUnit.SECONDS)          // timeout per call!
                        .exceptionally(ex -> {
                            log.warn("User call timed out: {}", ex.getMessage());
                            return getDefaultUser(userId);        // fallback!
                        });

        CompletableFuture<List<OrderDTO>> ordersFuture =
                CompletableFuture.supplyAsync(
                                () -> fetchOrders(userId), apiExecutor)
                        .orTimeout(5, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            log.warn("Orders call timed out: {}", ex.getMessage());
                            return Collections.emptyList();
                        });

        CompletableFuture<List<ProductDTO>> productsFuture =
                CompletableFuture.supplyAsync(
                                () -> fetchProducts(), apiExecutor)
                        .orTimeout(3, TimeUnit.SECONDS)
                        .exceptionally(ex -> Collections.emptyList());

        // Wait for all — with global timeout
        try {
            CompletableFuture.allOf(
                    userFuture, ordersFuture, productsFuture
            ).get(10, TimeUnit.SECONDS);   // global timeout
        } catch (TimeoutException ex) {
            log.error("Dashboard load timed out after 10s");
        } catch (Exception ex) {
            log.error("Dashboard load failed: {}", ex.getMessage());
        }

        return DashboardDTO.builder()
                .user(userFuture.getNow(null))
                .orders(ordersFuture.getNow(Collections.emptyList()))
                .products(productsFuture.getNow(Collections.emptyList()))
                .callDurationMs(System.currentTimeMillis() - start)
                .build();
    }

    /**
     * Chained calls — Call B uses result from Call A
     *
     * thenCompose = flatMap (returns CompletableFuture)
     * thenApply  = map     (transforms the value)
     */
    public DashboardDTO getDashboardChained(Long userId) {

        // Step 1: Fetch user
        // Step 2: Use user's department to fetch relevant products
        // Step 3: Combine results
        CompletableFuture<DashboardDTO> result =
                CompletableFuture
                        .supplyAsync(() -> fetchUser(userId), apiExecutor)
                        .thenCompose(user ->
                                // Now fetch products filtered by user's department
                                CompletableFuture.supplyAsync(
                                        () -> fetchProductsByDept(user.getDepartment()),
                                        apiExecutor
                                ).thenApply(products ->
                                        DashboardDTO.builder()
                                                .user(user)
                                                .products(products)
                                                .build()
                                )
                        );

        return result.join();
    }

    /**
     * Process a LIST of items in parallel.
     * E.g. fetch details for 100 product IDs simultaneously.
     */
    public List<ProductDTO> fetchManyProducts(List<Long> productIds) {

        List<CompletableFuture<ProductDTO>> futures = productIds.stream()
                .map(id -> CompletableFuture.supplyAsync(
                                () -> fetchProductById(id), apiExecutor)
                        .exceptionally(ex -> {
                            log.warn("Failed to fetch product {}: {}", id, ex.getMessage());
                            return null;    // null for failed ones
                        })
                )
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)   // remove failed nulls
                .toList();
    }

    // ── Private fetch methods ──────────────────────────

    private UserDTO fetchUser(Long id) {
        return restTemplate.getForObject(
                userServiceUrl + "/api/users/{id}", UserDTO.class, id);
    }

    private List<OrderDTO> fetchOrders(Long userId) {
        ResponseEntity<List<OrderDTO>> resp = restTemplate.exchange(
                orderServiceUrl + "/api/orders?userId={id}",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {}, userId);
        return resp.getBody() != null ? resp.getBody() : List.of();
    }

    private List<ProductDTO> fetchProducts() {
        ResponseEntity<List<ProductDTO>> resp = restTemplate.exchange(
                productServiceUrl + "/api/products",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});
        return resp.getBody() != null ? resp.getBody() : List.of();
    }

    private List<ProductDTO> fetchProductsByDept(String dept) {
        ResponseEntity<List<ProductDTO>> resp = restTemplate.exchange(
                productServiceUrl + "/api/products?dept={dept}",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {}, dept);
        return resp.getBody() != null ? resp.getBody() : List.of();
    }

    private ProductDTO fetchProductById(Long id) {
        return restTemplate.getForObject(
                productServiceUrl + "/api/products/{id}", ProductDTO.class, id);
    }

    private PaymentDTO fetchLatestPayment(Long userId) {
        return restTemplate.getForObject(
                paymentServiceUrl + "/api/payments/latest?userId={id}",
                PaymentDTO.class, userId);
    }

    private UserDTO getDefaultUser(Long id) {
        return UserDTO.builder().id(id).name("Unknown").build();
    }
}