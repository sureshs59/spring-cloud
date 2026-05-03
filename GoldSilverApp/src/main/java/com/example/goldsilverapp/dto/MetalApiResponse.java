package com.example.goldsilverapp.dto;

import java.util.Map;

public class MetalApiResponse {
    private boolean success;
    private String base;
    private Map<String, Double> rates;

    public MetalApiResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public String getBase() {
        return base;
    }

    public Map<String, Double> getRates() {
        return rates;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public void setRates(Map<String, Double> rates) {
        this.rates = rates;
    }
}
