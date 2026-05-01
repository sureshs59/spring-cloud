package com.example.restpatterns.service.batch;

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
import java.util.stream.Collectors;

/**
 * PATTERN 6 — Batch Processing
 * For large lists of IDs. Avoids overwhelming downstream services.
 * Three strategies: full-batch, chunked-parallel, semaphore-limited.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchApiService {

    private final RestTemplate restTemplate;
    private final Executor     apiExecutor;

    @Value("${api.product-service}") private String productServiceUrl;
    @Value("${api.user-service}")    private String userServiceUrl;

    // ────────────────────────────────────────────────────────
    //  Strategy A: Full Batch — send ALL IDs in ONE request
    //  Most efficient when API supports batch endpoints
    // ────────────────────────────────────────────────────────
    public List<ProductDTO> getProductsBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        String idParam = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        log.info("[PATTERN-6 Batch] Fetching {} products in 1 request", ids.size());

        ResponseEntity<List<ProductDTO>> response = restTemplate.exchange(
                productServiceUrl + "/api/products/batch?ids={ids}",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {}, idParam);

        return response.getBody() != null ? response.getBody() : List.of();
    }

    // ────────────────────────────────────────────────────────
    //  Strategy B: Chunked Parallel
    //  Split 1000 IDs into chunks of 50, process each chunk
    //  as one parallel API call. Avoids server overload.
    // ────────────────────────────────────────────────────────
    public List<ProductDTO> getProductsInChunks(List<Long> ids, int chunkSize) {
        if (ids == null || ids.isEmpty()) return List.of();

        List<List<Long>> chunks = partitionList(ids, chunkSize);
        log.info("[PATTERN-6 Chunked] {} IDs split into {} chunks of {}",
                ids.size(), chunks.size(), chunkSize);

        List<CompletableFuture<List<ProductDTO>>> futures = chunks.stream()
                .map(chunk -> CompletableFuture
                        .supplyAsync(() -> getProductsBatch(chunk), apiExecutor)
                        .exceptionally(ex -> {
                            log.error("[PATTERN-6] Chunk failed: {}", ex.getMessage());
                            return Collections.emptyList();
                        }))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(Collection::stream)
                .toList();
    }

    // ────────────────────────────────────────────────────────
    //  Strategy C: Semaphore-Limited Parallel
    //  All fire in parallel but max N concurrent at a time.
    //  Prevents connection pool exhaustion.
    // ────────────────────────────────────────────────────────
    public List<ProductDTO> getProductsSemaphoreLimited(List<Long> ids, int maxConcurrent) {
        if (ids == null || ids.isEmpty()) return List.of();

        Semaphore semaphore = new Semaphore(maxConcurrent);
        log.info("[PATTERN-6 Semaphore] {} products, max {} concurrent", ids.size(), maxConcurrent);

        List<CompletableFuture<ProductDTO>> futures = ids.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> {
                    try {
                        semaphore.acquire();          // wait for slot
                        try {
                            return restTemplate.getForObject(
                                    productServiceUrl + "/api/products/{id}",
                                    ProductDTO.class, id);
                        } finally {
                            semaphore.release();      // always release!
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return null;
                    } catch (Exception ex) {
                        log.warn("[PATTERN-6] Product {} failed: {}", id, ex.getMessage());
                        return null;
                    }
                }, apiExecutor))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();
    }

    // ────────────────────────────────────────────────────────
    //  Strategy D: Rate-Limited Sequential
    //  One call per interval — for strict rate-limited APIs.
    // ────────────────────────────────────────────────────────
    public List<ProductDTO> getProductsRateLimited(List<Long> ids, int callsPerSecond) {
        if (ids == null || ids.isEmpty()) return List.of();

        List<ProductDTO> results = new ArrayList<>();
        long delayMs = 1000L / callsPerSecond;
        log.info("[PATTERN-6 RateLimit] {} products at {}/sec, {}ms delay",
                ids.size(), callsPerSecond, delayMs);

        for (Long id : ids) {
            try {
                ProductDTO p = restTemplate.getForObject(
                        productServiceUrl + "/api/products/{id}", ProductDTO.class, id);
                if (p != null) results.add(p);
                Thread.sleep(delayMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                log.warn("[PATTERN-6] Product {} failed, skipping: {}", id, ex.getMessage());
            }
        }
        return results;
    }

    // ────────────────────────────────────────────────────────
    //  Batch users — fetch all user details for a list of IDs
    // ────────────────────────────────────────────────────────
    public List<UserDTO> getUsersBatch(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();

        // Chunk into groups of 20 and fetch in parallel
        return getChunkedUsers(userIds, 20);
    }

    private List<UserDTO> getChunkedUsers(List<Long> ids, int chunkSize) {
        List<List<Long>> chunks = partitionList(ids, chunkSize);

        List<CompletableFuture<List<UserDTO>>> futures = chunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(() ->
                                chunk.stream()
                                        .map(id -> {
                                            try {
                                                return restTemplate.getForObject(
                                                        userServiceUrl + "/api/users/{id}", UserDTO.class, id);
                                            } catch (Exception ex) {
                                                return (UserDTO) null;
                                            }
                                        })
                                        .filter(Objects::nonNull)
                                        .toList(),
                        apiExecutor))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(Collection::stream)
                .toList();
    }

    // ── Utility: partition list into equal chunks ─────────
    public <T> List<List<T>> partitionList(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size)
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        return partitions;
    }
}