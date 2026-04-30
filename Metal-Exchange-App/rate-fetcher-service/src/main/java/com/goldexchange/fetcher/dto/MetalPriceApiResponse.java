package com.goldexchange.fetcher.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Maps the MetalPriceAPI JSON response.
 * Sample: https://api.metalpriceapi.com/v1/latest?api_key=KEY&base=INR&currencies=XAU,XAG
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetalPriceApiResponse {

    private boolean success;
    private String base;
    private long timestamp;

    /** Map of symbol → rate. XAU = gold ounce, XAG = silver ounce in INR */
    private Map<String, BigDecimal> rates;

    // Convenience accessors
    public BigDecimal getGoldOunceInBase()  { return rates != null ? rates.get("XAU") : null; }
    public BigDecimal getSilverOunceInBase(){ return rates != null ? rates.get("XAG") : null; }
}
