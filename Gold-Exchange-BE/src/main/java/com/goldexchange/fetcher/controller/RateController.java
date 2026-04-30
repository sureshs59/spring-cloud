package com.goldexchange.fetcher.controller;

import com.goldexchange.fetcher.model.MetalRate;
import com.goldexchange.fetcher.model.MetalRate.MetalType;
import com.goldexchange.fetcher.service.RateFetcherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for gold/silver rate data.
 *
 * All endpoints use CORS-friendly JSON responses.
 * The Angular frontend calls these directly (or via API Gateway).
 */
@RestController
@RequestMapping("/api/rates")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class RateController {

    private final RateFetcherService rateFetcherService;

    /** GET /api/rates/today — returns both gold and silver for today */
    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodayRates() {
        var gold   = rateFetcherService.getLatestRate(MetalType.GOLD);
        var silver = rateFetcherService.getLatestRate(MetalType.SILVER);

        return ResponseEntity.ok(Map.of(
            "gold",      gold.stream().findAny().orElse(null),
            "silver",    silver.stream().findAny().orElse(null),
            "timestamp", System.currentTimeMillis()
        ));
    }

    /** GET /api/rates/gold/latest */
    @GetMapping("/gold/latest")
    public ResponseEntity<List<MetalRate>> getLatestGold() {
        return ResponseEntity.ok(rateFetcherService.getLatestRate(MetalType.GOLD));

    }

    /** GET /api/rates/silver/latest */
    @GetMapping("/silver/latest")
    public ResponseEntity<List<MetalRate>> getLatestSilver() {
        return ResponseEntity.ok(rateFetcherService.getLatestRate(MetalType.SILVER));
//                rateFetcherService.getLatestRate(MetalType.SILVER)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/rates/gold/history?days=30
     * Returns list of daily gold rates for charting
     */
    @GetMapping("/gold/history")
    public ResponseEntity<List<MetalRate>> getGoldHistory(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(rateFetcherService.getHistoricalRates(MetalType.GOLD, days));
    }

    /** GET /api/rates/silver/history?days=30 */
    @GetMapping("/silver/history")
    public ResponseEntity<List<MetalRate>> getSilverHistory(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(rateFetcherService.getHistoricalRates(MetalType.SILVER, days));
    }

    /**
     * GET /api/rates/all?days=30
     * Returns both metals for the dashboard chart
     */
    @GetMapping("/all")
    public ResponseEntity<List<MetalRate>> getAllRates(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(rateFetcherService.getAllRatesSince(days));
    }

    /**
     * POST /api/rates/fetch — manual trigger for testing
     * In production this is called only by the scheduler
     */
    @PostMapping("/fetch")
    public ResponseEntity<Map<String, Object>> triggerFetch() {
        var saved = rateFetcherService.fetchAndStoreRates();
        return ResponseEntity.ok(Map.of(
            "message", "Fetch complete",
            "recordsSaved", saved.size(),
            "rates", saved
        ));
    }
}
