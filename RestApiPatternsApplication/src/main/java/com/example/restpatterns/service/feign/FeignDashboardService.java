package com.example.restpatterns.service.feign;

import com.example.restpatterns.client.OrderClient;
import com.example.restpatterns.client.PaymentClient;
import com.example.restpatterns.client.ProductClient;
import com.example.restpatterns.client.UserClient;
import com.example.restpatterns.dto.Dtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * PATTERN 4 — Feign Client (Declarative)
 * Feign is synchronous by default.
 * Wrap with CompletableFuture for parallel execution.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeignDashboardService {

    @Qualifier("user-service")
    private final UserClient    userClient;
    @Qualifier("product-service")
    private final ProductClient productClient;
    @Qualifier("order-service")
    private final OrderClient   orderClient;
    @Qualifier("payment-service")
    private final PaymentClient paymentClient;
    private final Executor      apiExecutor;

    /** Feign sequential — synchronous one after another */
    public DashboardDTO getDashboard(Long userId) {
        long start = System.currentTimeMillis();
        log.info("[PATTERN-4 Feign] Sequential for userId={}", userId);

        UserDTO          user     = userClient.getUserById(userId);
        List<OrderDTO>   orders   = orderClient.getOrdersByUserId(userId);
        List<ProductDTO> products = productClient.getAllProducts();
        PaymentDTO       payment  = paymentClient.getLatestPayment(userId);

        return DashboardDTO.builder()
                .user(user).orders(orders).products(products).payment(payment)
                .pattern("Pattern 4 — Feign Sequential")
                .callDurationMs(System.currentTimeMillis() - start).build();
    }

    /** Feign + CompletableFuture = parallel Feign calls */
    public DashboardDTO getDashboardParallel(Long userId) {
        long start = System.currentTimeMillis();
        log.info("[PATTERN-4 Feign] Parallel for userId={}", userId);

        CompletableFuture<UserDTO>          uF = CompletableFuture.supplyAsync(() -> userClient.getUserById(userId),       apiExecutor);
        CompletableFuture<List<OrderDTO>>   oF = CompletableFuture.supplyAsync(() -> orderClient.getOrdersByUserId(userId),apiExecutor);
        CompletableFuture<List<ProductDTO>> pF = CompletableFuture.supplyAsync(() -> productClient.getAllProducts(),        apiExecutor);
        CompletableFuture<PaymentDTO>       pyF= CompletableFuture.supplyAsync(() -> paymentClient.getLatestPayment(userId),apiExecutor);

        CompletableFuture.allOf(uF, oF, pF, pyF).join();

        return DashboardDTO.builder()
                .user(uF.join()).orders(oF.join()).products(pF.join()).payment(pyF.join())
                .pattern("Pattern 4 — Feign Parallel")
                .callDurationMs(System.currentTimeMillis() - start).build();
    }
}