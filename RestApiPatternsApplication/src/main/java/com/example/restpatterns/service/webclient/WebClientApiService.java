package com.example.restpatterns.service.webclient;

import com.example.restpatterns.dto.Dtos.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * PATTERN 3 — WebClient (Reactive / Non-Blocking)
 * Best for high-concurrency. Threads are never blocked waiting.
 */
@Service
@Slf4j
public class WebClientApiService {

    private final WebClient userClient;
    private final WebClient productClient;
    private final WebClient orderClient;
    private final WebClient paymentClient;

    public WebClientApiService(
            @Qualifier("userWebClient")    WebClient userClient,
            @Qualifier("productWebClient") WebClient productClient,
            @Qualifier("orderWebClient")   WebClient orderClient,
            @Qualifier("paymentWebClient") WebClient paymentClient) {
        this.userClient    = userClient;
        this.productClient = productClient;
        this.orderClient   = orderClient;
        this.paymentClient = paymentClient;
    }

    // ── Parallel via Mono.zip ─────────────────────────────
    public Mono<DashboardDTO> getDashboardParallel(Long userId) {
        long start = System.currentTimeMillis();
        log.info("[PATTERN-3 WebClient] zip() parallel for userId={}", userId);

        Mono<UserDTO>          userMono     = getUser(userId);
        Mono<List<OrderDTO>>   ordersMono   = getOrders(userId);
        Mono<List<ProductDTO>> productsMono = getProducts();
        Mono<PaymentDTO>       paymentMono  = getPayment(userId);

        return Mono.zip(userMono, ordersMono, productsMono, paymentMono)
                .map(t -> DashboardDTO.builder()
                        .user(t.getT1()).orders(t.getT2())
                        .products(t.getT3()).payment(t.getT4())
                        .pattern("Pattern 3 — WebClient Reactive Parallel")
                        .callDurationMs(System.currentTimeMillis() - start)
                        .build())
                .doOnSuccess(d -> log.info("[PATTERN-3] Done in {}ms", d.getCallDurationMs()))
                .doOnError(e -> log.error("[PATTERN-3] Error: {}", e.getMessage()));
    }

    // ── Chained: Call B uses result of Call A ─────────────
    public Mono<DashboardDTO> getDashboardChained(Long userId) {
        return getUser(userId)
                .zipWhen(user -> getProducts())
                .map(t -> DashboardDTO.builder()
                        .user(t.getT1()).products(t.getT2())
                        .pattern("Pattern 3 — WebClient Chained").build());
    }

    // ── Sequential dependent steps with flatMap ───────────
    public Mono<OrderDTO> createAndConfirmOrder(Long userId, Long productId) {
        return getUser(userId)
                .flatMap(user -> getProduct(productId)
                        .flatMap(product -> placeOrder(
                                userId, productId, product.getPrice())));
    }

    // ── Stream many items concurrently via Flux ───────────
    public Flux<ProductDTO> fetchProductsFlux(List<Long> ids) {
        return Flux.fromIterable(ids)
                .flatMap(id -> getProduct(id)
                                .onErrorResume(ex -> { log.warn("Product {} skipped", id); return Mono.empty(); }),
                        10); // max 10 concurrent
    }

    // ── Sync wrapper (blocks — use sparingly) ────────────
    public DashboardDTO getDashboardSync(Long userId) {
        return getDashboardParallel(userId).block();
    }

    // ── Individual reactive getters ───────────────────────
    public Mono<UserDTO> getUser(Long id) {
        return userClient.get().uri("/api/users/{id}", id)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, r -> Mono.error(new RuntimeException("User not found: " + id)))
                .onStatus(HttpStatus::is5xxServerError, r -> Mono.error(new RuntimeException("User service error")))
                .bodyToMono(UserDTO.class)
                .timeout(Duration.ofSeconds(5))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(300)))
                .onErrorReturn(UserDTO.builder().id(id).name("Unavailable").build());
    }

    public Mono<List<OrderDTO>> getOrders(Long userId) {
        return orderClient.get().uri("/api/orders?userId={id}", userId)
                .retrieve()
                .bodyToFlux(OrderDTO.class)
                .collectList()
                .onErrorReturn(Collections.emptyList());
    }

    public Mono<List<ProductDTO>> getProducts() {
        return productClient.get().uri("/api/products")
                .retrieve()
                .bodyToFlux(ProductDTO.class)
                .collectList()
                .onErrorReturn(Collections.emptyList());
    }

    public Mono<ProductDTO> getProduct(Long id) {
        return productClient.get().uri("/api/products/{id}", id)
                .retrieve()
                .bodyToMono(ProductDTO.class);
    }

    public Mono<PaymentDTO> getPayment(Long userId) {
        return paymentClient.get().uri("/api/payments/latest?userId={id}", userId)
                .retrieve()
                .bodyToMono(PaymentDTO.class)
                .onErrorReturn(PaymentDTO.builder().status("UNAVAILABLE").build());
    }

    private Mono<OrderDTO> placeOrder(Long userId, Long productId, Double price) {
        OrderDTO req = OrderDTO.builder().userId(userId).productId(productId).totalAmount(price).build();
        return orderClient.post().uri("/api/orders").bodyValue(req)
                .retrieve().bodyToMono(OrderDTO.class);
    }
}