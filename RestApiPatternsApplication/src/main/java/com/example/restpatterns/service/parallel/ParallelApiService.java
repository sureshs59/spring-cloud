package com.example.restpatterns.service.parallel;

import com.example.restpatterns.dto.Dtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;

/**
 * PATTERN 2 — Parallel calls using CompletableFuture
 * Total time = max(individual times), NOT sum!
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParallelApiService {

    private final RestTemplate restTemplate;
    private final Executor     apiExecutor;

    @Value("${api.user-service}")    private String userServiceUrl;
    @Value("${api.product-service}") private String productServiceUrl;
    @Value("${api.order-service}")   private String orderServiceUrl;
    @Value("${api.payment-service}") private String paymentServiceUrl;

    public DashboardDTO getDashboardParallel(Long userId) {
        long start = System.currentTimeMillis();
        log.info("[PATTERN-2 Parallel] Firing 4 calls simultaneously for userId={}", userId);

        CompletableFuture<UserDTO>          userFuture    = CompletableFuture.supplyAsync(() -> fetchUser(userId),    apiExecutor);
        CompletableFuture<List<OrderDTO>>   ordersFuture  = CompletableFuture.supplyAsync(() -> fetchOrders(userId),  apiExecutor);
        CompletableFuture<List<ProductDTO>> productsFuture= CompletableFuture.supplyAsync(() -> fetchProducts(),      apiExecutor);
        CompletableFuture<PaymentDTO>       paymentFuture = CompletableFuture.supplyAsync(() -> fetchPayment(userId), apiExecutor);

        CompletableFuture.allOf(userFuture, ordersFuture, productsFuture, paymentFuture).join();

        long duration = System.currentTimeMillis() - start;
        log.info("[PATTERN-2 Parallel] Completed in {}ms (max of all calls)", duration);

        return DashboardDTO.builder()
                .user(userFuture.join()).orders(ordersFuture.join())
                .products(productsFuture.join()).payment(paymentFuture.join())
                .pattern("Pattern 2 — Parallel CompletableFuture")
                .callDurationMs(duration).build();
    }

    public DashboardDTO getDashboardWithTimeout(Long userId) {
        long start = System.currentTimeMillis();

        CompletableFuture<UserDTO> userFuture =
                CompletableFuture.supplyAsync(() -> fetchUser(userId), apiExecutor)
                        .orTimeout(5, TimeUnit.SECONDS)
                        .exceptionally(ex -> { log.warn("User timed out"); return UserDTO.builder().id(userId).name("Unavailable").build(); });

        CompletableFuture<List<OrderDTO>> ordersFuture =
                CompletableFuture.supplyAsync(() -> fetchOrders(userId), apiExecutor)
                        .orTimeout(5, TimeUnit.SECONDS)
                        .exceptionally(ex -> Collections.emptyList());

        CompletableFuture<List<ProductDTO>> productsFuture =
                CompletableFuture.supplyAsync(() -> fetchProducts(), apiExecutor)
                        .orTimeout(3, TimeUnit.SECONDS)
                        .exceptionally(ex -> Collections.emptyList());

        CompletableFuture<PaymentDTO> paymentFuture =
                CompletableFuture.supplyAsync(() -> fetchPayment(userId), apiExecutor)
                        .orTimeout(3, TimeUnit.SECONDS)
                        .exceptionally(ex -> PaymentDTO.builder().status("UNAVAILABLE").build());

        try {
            CompletableFuture.allOf(userFuture, ordersFuture, productsFuture, paymentFuture)
                    .get(10, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            log.error("[PATTERN-2] Global timeout after 10s");
        } catch (Exception ex) {
            log.error("[PATTERN-2] Error: {}", ex.getMessage());
        }

        return DashboardDTO.builder()
                .user(userFuture.getNow(null))
                .orders(ordersFuture.getNow(Collections.emptyList()))
                .products(productsFuture.getNow(Collections.emptyList()))
                .payment(paymentFuture.getNow(null))
                .pattern("Pattern 2 — Parallel with Timeout")
                .callDurationMs(System.currentTimeMillis() - start).build();
    }

    public DashboardDTO getDashboardChained(Long userId) {
        return CompletableFuture
                .supplyAsync(() -> fetchUser(userId), apiExecutor)
                .thenCompose(user ->
                        CompletableFuture.supplyAsync(() -> fetchProducts(), apiExecutor)
                                .thenApply(products -> DashboardDTO.builder()
                                        .user(user).products(products)
                                        .pattern("Pattern 2 — Chained CompletableFuture").build())
                ).join();
    }

    public List<ProductDTO> fetchManyProductsParallel(List<Long> ids) {
        List<CompletableFuture<ProductDTO>> futures = ids.stream()
                .map(id -> CompletableFuture.supplyAsync(() ->
                                        restTemplate.getForObject(productServiceUrl + "/api/products/{id}", ProductDTO.class, id),
                                apiExecutor)
                        .exceptionally(ex -> { log.warn("Product {} failed", id); return null; }))
                .toList();
        return futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).toList();
    }

    private UserDTO fetchUser(Long id) {
        return restTemplate.getForObject(userServiceUrl + "/api/users/{id}", UserDTO.class, id);
    }
    private List<OrderDTO> fetchOrders(Long userId) {
        ResponseEntity<List<OrderDTO>> r = restTemplate.exchange(
                orderServiceUrl + "/api/orders?userId={id}", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {}, userId);
        return r.getBody() != null ? r.getBody() : List.of();
    }
    private List<ProductDTO> fetchProducts() {
        ResponseEntity<List<ProductDTO>> r = restTemplate.exchange(
                productServiceUrl + "/api/products", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});
        return r.getBody() != null ? r.getBody() : List.of();
    }
    private PaymentDTO fetchPayment(Long userId) {
        return restTemplate.getForObject(paymentServiceUrl + "/api/payments/latest?userId={id}", PaymentDTO.class, userId);
    }
}