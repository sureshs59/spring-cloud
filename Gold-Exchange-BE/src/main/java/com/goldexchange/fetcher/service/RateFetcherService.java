package com.goldexchange.fetcher.service;

import com.goldexchange.fetcher.dto.MetalPriceApiResponse;
import com.goldexchange.fetcher.model.MetalRate;
import com.goldexchange.fetcher.model.MetalRate.MetalType;
import com.goldexchange.fetcher.repository.MetalRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Core service that:
 * 1. Calls MetalPriceAPI to get live XAU/XAG rates in INR
 * 2. Converts ounce prices to gram prices (1 troy ounce = 31.1035 grams)
 * 3. Calculates daily change vs previous day
 * 4. Saves to PostgreSQL — skips if today's rate already exists
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateFetcherService {

    private static final BigDecimal GRAMS_PER_OUNCE = new BigDecimal("31.1035");

    private final WebClient webClient;
    private final MetalRateRepository rateRepository;

    @Value("${metal-price-api.api-key}")
    private String apiKey;

    @Value("${metal-price-api.base-currency}")
    private String baseCurrency;

    /**
     * Main fetch-and-store method.
     * Called by the scheduler and also available via REST for manual trigger.
     */
    @Transactional
    public List<MetalRate> fetchAndStoreRates() {
        log.info("Starting rate fetch from MetalPriceAPI — base currency: {}", baseCurrency);

        MetalPriceApiResponse response = callApi();
        if (response == null || !response.isSuccess()) {
            log.error("MetalPriceAPI returned failure or null response");
            return List.of();
        }

        LocalDate today = LocalDate.now();
        List<MetalRate> savedRates = new ArrayList<>();

        savedRates.addAll(processGoldRate(response, today));
        savedRates.addAll(processSilverRate(response, today));

        log.info("Rate fetch complete — saved {} records for {}", savedRates.size(), today);
        return savedRates;
    }

    private List<MetalRate> processGoldRate(MetalPriceApiResponse response, LocalDate today) {
        BigDecimal goldOunceInr = response.getGoldOunceInBase();
        if (goldOunceInr == null) { log.warn("No XAU rate in response"); return List.of(); }

        // MetalPriceAPI returns how many INR per 1 XAU (ounce of gold)
        // Invert if base=INR: API gives INR per ounce directly
        BigDecimal pricePerGram = goldOunceInr.divide(GRAMS_PER_OUNCE, 2, RoundingMode.HALF_UP);
        return saveRate(MetalType.GOLD, today, goldOunceInr, pricePerGram);
    }

    private List<MetalRate> processSilverRate(MetalPriceApiResponse response, LocalDate today) {
        BigDecimal silverOunceInr = response.getSilverOunceInBase();
        if (silverOunceInr == null) { log.warn("No XAG rate in response"); return List.of(); }

        BigDecimal pricePerGram = silverOunceInr.divide(GRAMS_PER_OUNCE, 2, RoundingMode.HALF_UP);
        return saveRate(MetalType.SILVER, today, silverOunceInr, pricePerGram);
    }

    private List<MetalRate> saveRate(MetalType metal, LocalDate today,
                                     BigDecimal ouncePrice, BigDecimal gramPrice) {
        // Skip if we already stored today's rate (idempotent)
        if (rateRepository.existsByRateDateAndMetal(today, metal)) {
            log.info("{} rate for {} already exists — skipping", metal, today);
            return List.of();
        }

        // Calculate change vs yesterday
        BigDecimal changeInr = BigDecimal.ZERO;
        BigDecimal changePercent = BigDecimal.ZERO;
        Optional<MetalRate> yesterday = rateRepository
                .findByRateDateAndMetal(today.minusDays(1), metal);
        if (yesterday.isPresent()) {
            changeInr = gramPrice.subtract(yesterday.get().getPricePerGramInr())
                                 .setScale(2, RoundingMode.HALF_UP);
            if (yesterday.get().getPricePerGramInr().compareTo(BigDecimal.ZERO) > 0) {
                changePercent = changeInr
                        .divide(yesterday.get().getPricePerGramInr(), 6, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(3, RoundingMode.HALF_UP);
            }
        }

        MetalRate rate = MetalRate.builder()
                .rateDate(today)
                .metal(metal)
                .pricePerGramInr(gramPrice)
                .pricePer10GramInr(gramPrice.multiply(BigDecimal.TEN).setScale(2, RoundingMode.HALF_UP))
                .pricePerOunceInr(ouncePrice.setScale(2, RoundingMode.HALF_UP))
                .changeInr(changeInr)
                .changePercent(changePercent)
                .fetchedAt(LocalDateTime.now())
                .source("metalpriceapi.com")
                .build();

        MetalRate saved = rateRepository.save(rate);
        log.info("Saved {} rate: ₹{}/gram (change: {}%)", metal, gramPrice, changePercent);
        return List.of(saved);
    }

    private MetalPriceApiResponse callApi() {
        try {
            String url = "/latest?api_key={key}&base={base}&currencies=XAU,XAG";
            return webClient.get()
                    .uri(url, apiKey, baseCurrency)
                    .retrieve()
                    .bodyToMono(MetalPriceApiResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to call MetalPriceAPI: {}", e.getMessage());
            return null;
        }
    }

    /** Get today's rates (called by controller for API responses) */
    @Transactional(readOnly = true)
    public Optional<MetalRate> getTodayRate(MetalType metal) {
        return rateRepository.findByRateDateAndMetal(LocalDate.now(), metal);
    }

    /** Get the latest available rate regardless of date */
    @Transactional(readOnly = true)
    public List<MetalRate> getLatestRate(MetalType metal) {
        return rateRepository.findLatestByMetal(metal);
    }

    /** Get rates for last N days */
    @Transactional(readOnly = true)
    public List<MetalRate> getHistoricalRates(MetalType metal, int days) {
        return rateRepository.findByMetalAndDateRange(metal, LocalDate.now().minusDays(days));
    }

    /** Get all rates since a specific date (both metals) */
    @Transactional(readOnly = true)
    public List<MetalRate> getAllRatesSince(int days) {
        return rateRepository.findAllSince(LocalDate.now().minusDays(days));
    }
}
