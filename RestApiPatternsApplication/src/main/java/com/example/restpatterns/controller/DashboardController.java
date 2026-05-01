package com.example.restpatterns.controller;

import com.example.restpatterns.dto.Dtos;
import com.example.restpatterns.dto.Dtos.*;
import com.example.restpatterns.service.batch.BatchApiService;
import com.example.restpatterns.service.feign.FeignDashboardService;
import com.example.restpatterns.service.parallel.ParallelApiService;
import com.example.restpatterns.service.sequential.SequentialApiService;
import com.example.restpatterns.service.webclient.WebClientApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final SequentialApiService sequentialSvc;
    private final ParallelApiService parallelSvc;
    private final WebClientApiService webClientSvc;
    private final FeignDashboardService feignSvc;
    private final BatchApiService batchSvc;

    /**
     * Sequential — simple, slow
     */
    @GetMapping("/sequential/{userId}")
    public ResponseEntity<ApiResponse<DashboardDTO>> sequential(
            @PathVariable Long userId) {
        Dtos.DashboardDTO data = sequentialSvc.getDashboardSequential(userId);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * Parallel — fast, uses CompletableFuture
     */
    @GetMapping("/parallel/{userId}")
    public ResponseEntity<ApiResponse<DashboardDTO>> parallel(
            @PathVariable Long userId) {
        DashboardDTO data = parallelSvc.getDashboardParallel(userId);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * Parallel with timeout — safe for prod
     */
    @GetMapping("/parallel-safe/{userId}")
    public ResponseEntity<ApiResponse<DashboardDTO>> parallelSafe(
            @PathVariable Long userId) {
        DashboardDTO data = parallelSvc.getDashboardWithTimeout(userId);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * WebClient reactive — non-blocking, high concurrency
     */
    @GetMapping("/reactive/{userId}")
    public Mono<ResponseEntity<ApiResponse<DashboardDTO>>> reactive(
            @PathVariable Long userId) {
        return webClientSvc.getDashboardParallel(userId)
                .map(data -> ResponseEntity.ok(ApiResponse.ok(data)))
                .onErrorReturn(ResponseEntity
                        .status(500)
                        .body(ApiResponse.error("Failed to load dashboard")));
    }

    /**
     * Feign — declarative, cleanest code
     */
    @GetMapping("/feign/{userId}")
    public ResponseEntity<ApiResponse<DashboardDTO>> feign(
            @PathVariable Long userId) {
        DashboardDTO data = feignSvc.getDashboard(userId);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * Batch — many product IDs at once
     */
    @PostMapping("/products/batch")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> batchProducts(
            @RequestBody List<Long> productIds) {
        // Process in chunks of 50
        List<ProductDTO> products =
                batchSvc.getProductsInChunks(productIds, 50);
        return ResponseEntity.ok(ApiResponse.ok(products));
    }
}