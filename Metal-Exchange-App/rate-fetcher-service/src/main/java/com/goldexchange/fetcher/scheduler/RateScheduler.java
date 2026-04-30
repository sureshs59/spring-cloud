package com.goldexchange.fetcher.scheduler;

import com.goldexchange.fetcher.service.RateFetcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks that automatically fetch gold and silver rates.
 *
 * Schedule:
 *  - 10:00 AM IST daily (04:30 UTC) — main daily rate fetch
 *  - Every 4 hours Mon-Fri during market hours — intraday updates
 *  - On startup (1 second delay) — fetch immediately so app has data
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class RateScheduler {

    private final RateFetcherService rateFetcherService;

    /** Daily fetch at 10:00 AM IST */
    @Scheduled(cron = "${scheduler.cron}", zone = "Asia/Kolkata")
    public void fetchDailyRates() {
        log.info("=== SCHEDULED DAILY RATE FETCH starting ===");
        try {
            var saved = rateFetcherService.fetchAndStoreRates();
            log.info("=== SCHEDULED DAILY RATE FETCH complete — {} records saved ===", saved.size());
        } catch (Exception e) {
            log.error("Scheduled fetch failed: {}", e.getMessage(), e);
        }
    }

    /** Intraday refresh every 4 hours on weekdays */
    @Scheduled(cron = "${scheduler.intraday-cron}", zone = "Asia/Kolkata")
    public void fetchIntradayRates() {
        log.info("=== INTRADAY RATE REFRESH ===");
        try {
            rateFetcherService.fetchAndStoreRates();
        } catch (Exception e) {
            log.error("Intraday fetch failed: {}", e.getMessage());
        }
    }

    /** Fetch on startup so the app has data immediately */
    @Scheduled(initialDelay = 1000, fixedDelay = Long.MAX_VALUE)
    public void fetchOnStartup() {
        log.info("=== STARTUP RATE FETCH ===");
        try {
            rateFetcherService.fetchAndStoreRates();
        } catch (Exception e) {
            log.error("Startup fetch failed: {} — app will work with existing DB data", e.getMessage());
        }
    }
}
