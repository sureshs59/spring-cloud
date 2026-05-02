package com.goldexchange.fetcher.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Stores a single daily rate snapshot for one metal.
 * One row = one metal (GOLD or SILVER) on one date.
 *
 * Prices are stored in INR per gram (most useful unit for Indian users).
 * Raw ounce price is also stored for conversion reference.
 */
@Entity
@Table(
    name = "metal_prices",
    uniqueConstraints = @UniqueConstraint(columnNames = {"rate_date", "metal"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MetalRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Date this rate belongs to */
    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    /** GOLD or SILVER */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MetalType metal;

    /** Price per gram in INR (most used unit in India) */
    @Column(name = "price_per_gram_inr", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerGramInr;

    /** Price per 10 grams in INR (standard jewellery unit) */
    @Column(name = "price_per_10gram_inr", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePer10GramInr;

    /** Price per troy ounce in INR (raw API value × INR rate) */
    @Column(name = "price_per_ounce_inr", nullable = false, precision = 14, scale = 2)
    private BigDecimal pricePerOunceInr;

    /** Price per troy ounce in USD (from API) */
    @Column(name = "price_per_ounce_usd", precision = 12, scale = 4)
    private BigDecimal pricePerOunceUsd;

    /** USD to INR exchange rate used for conversion */
    @Column(name = "usd_to_inr_rate", precision = 8, scale = 4)
    private BigDecimal usdToInrRate;

    /** Daily change in INR per gram vs previous day */
    @Column(name = "change_inr", precision = 10, scale = 2)
    private BigDecimal changeInr;

    /** Daily change percentage */
    @Column(name = "change_percent", precision = 6, scale = 3)
    private BigDecimal changePercent;

    /** Day's high (per gram INR) */
    @Column(name = "day_high_inr", precision = 12, scale = 2)
    private BigDecimal dayHighInr;

    /** Day's low (per gram INR) */
    @Column(name = "day_low_inr", precision = 12, scale = 2)
    private BigDecimal dayLowInr;

    /** Opening price (per gram INR) */
    @Column(name = "open_price_inr", precision = 12, scale = 2)
    private BigDecimal openPriceInr;

    /** When this record was fetched from the API */
    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    /** API source used */
    @Column(name = "source", length = 50)
    private String source;

    public enum MetalType {
        GOLD, SILVER
    }
}
