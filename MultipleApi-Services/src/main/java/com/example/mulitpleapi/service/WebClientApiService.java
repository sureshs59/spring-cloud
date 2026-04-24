package com.example.mulitpleapi.service;

import com.example.mulitpleapi.dto.*;
import com.example.mulitpleapi.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.naming.ServiceUnavailableException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class WebClientApiService {

    private final WebClient userClient;
    private final WebClient productClient;
    private final WebClient orderClient;

    public WebClientApiService(
            @Qualifier("userWebClient") WebClient userClient,
            @Qualifier("productWebClient") WebClient productClient,
            @Qualifier("orderWebClient")   WebClient orderClient) {
        this.userClient    = userClient;
        this.productClient = productClient;
        this.orderClient   = orderClient;
    }

    // ── 1. Simple GET — async (returns Mono) ──────────

    public Mono<UserDTO> getUser(Long id) {
        return userClient.get()
                .uri("/api/users/{id}", id)
                .retrieve()
                .onStatus(
                        org.springframework.http.HttpStatusCode::is4xxClientError,
                        resp -> resp.bodyToMono(String.class)
                                .map(body -> new ResourceNotFoundException(
                                        "User not found: " + id))
                )
                .onStatus(
                        org.springframework.http.HttpStatusCode::is5xxServerError,
                        resp -> Mono.error(
                                new ServiceUnavailableException("User service down"))
                )
                .bodyToMono(UserDTO.class)
                .timeout(Duration.ofSeconds(5))
                .retry(2)               // retry on failure, 2 times
                .doOnError(ex -> log.error("Get user failed: {}", ex.getMessage()))
                .onErrorReturn(getDefaultUser(id));    // fallback value
    }

    // ── 2. Parallel calls — BEST PATTERN ──────────────

    /**
     * zip() — fires ALL simultaneously, waits for ALL.
     * Like Promise.all() in JavaScript.
     *
     * Sequential: 300+200+150 = 650ms
     * With zip():  max(300,200,150) = 300ms
     */
    public Mono<DashboardDTO> getDashboardParallel(Long userId) {

        Mono<UserDTO>          userMono    = getUser(userId);
        Mono<List<OrderDTO>>   ordersMono  = getOrders(userId);
        Mono<List<ProductDTO>> productsMono = getProducts();

        // All 3 fire simultaneously!
        return Mono.zip(userMono, ordersMono, productsMono)
                .map(tuple -> DashboardDTO.builder()
                        .user(tuple.getT1())
                        .orders(tuple.getT2())
                        .products(tuple.getT3())
                        .build()
                )
                .doOnSuccess(d ->
                        log.info("Dashboard loaded for user={}",
                                d.getUser().getName()))
                .doOnError(ex ->
                        log.error("Dashboard load failed: {}", ex.getMessage()));
    }

    /**
     * zipWhen() — chain dependent calls.
     * Call B uses result of Call A.
     */
    public Mono<DashboardDTO> getDashboardChained(Long userId) {
        return getUser(userId)
                .zipWhen(user ->
                        // Use user's department to get relevant products
                        getProductsByDept(user.getDepartment())
                )
                .map(tuple -> DashboardDTO.builder()
                        .user(tuple.getT1())
                        .products(tuple.getT2())
                        .build()
                );
    }

    /**
     * flatMap() — sequential dependent calls.
     * Step 2 uses result from Step 1.
     */
    public Mono<OrderDTO> createAndConfirmOrder(
            Long userId, Long productId) {

        return getUser(userId)           // Step 1: verify user
                .flatMap(user ->
                        getProduct(productId)    // Step 2: verify product
                                .flatMap(product ->
                                        placeOrder(          // Step 3: place order
                                                userId, productId,
                                                product.getPrice()
                                        )
                                )
                );
    }

    /**
     * Flux — process a stream of items reactively.
     * Fetch details for many product IDs.
     */
    public Flux<ProductDTO> fetchAllProducts(List<Long> ids) {
        return Flux.fromIterable(ids)
                .flatMap(id ->              // parallel by default!
                                getProduct(id)
                                        .onErrorResume(ex -> {
                                            log.warn("Product {} failed, skipping", id);
                                            return Mono.empty();   // skip failed ones
                                        }),
                        10                     // max concurrency = 10 at once
                );
    }

    /**
     * Block — use when you MUST return synchronously.
     * Converts Mono/Flux to sync — use sparingly!
     */
    public DashboardDTO getDashboardSync(Long userId) {
        return getDashboardParallel(userId).block();
    }

    // ── Getters used above ────────────────────────────

    public Mono<List<OrderDTO>> getOrders(Long userId) {
        return orderClient.get()
                .uri("/api/orders?userId={id}", userId)
                .retrieve()
                .bodyToFlux(OrderDTO.class)
                .collectList()
                .onErrorReturn(Collections.emptyList());
    }

    public Mono<List<ProductDTO>> getProducts() {
        return productClient.get()
                .uri("/api/products")
                .retrieve()
                .bodyToFlux(ProductDTO.class)
                .collectList()
                .onErrorReturn(Collections.emptyList());
    }

    public Mono<List<ProductDTO>> getProductsByDept(String dept) {
        return productClient.get()
                .uri(b -> b.path("/api/products").queryParam("dept", dept).build())
                .retrieve()
                .bodyToFlux(ProductDTO.class)
                .collectList()
                .onErrorReturn(Collections.emptyList());
    }

    public Mono<ProductDTO> getProduct(Long id) {
        return productClient.get()
                .uri("/api/products/{id}", id)
                .retrieve()
                .bodyToMono(ProductDTO.class);
    }

    private Mono<OrderDTO> placeOrder(Long userId, Long productId, Double price) {
        OrderDTO req = OrderDTO.builder()
                .userId(userId).productId(productId)
                .totalAmount(price).build();
        return orderClient.post()
                .uri("/api/orders")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(OrderDTO.class);
    }

    private UserDTO getDefaultUser(Long id) {
        return UserDTO.builder().id(id).name("Unknown").build();
    }
}
