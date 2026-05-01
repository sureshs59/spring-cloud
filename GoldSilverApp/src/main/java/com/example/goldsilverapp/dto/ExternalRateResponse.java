package com.example.goldsilverapp.dto;

public class ExternalRateResponse {
    private String metal;
    private double price;
    private String unit;

    public ExternalRateResponse() {
    }

    public ExternalRateResponse(String metal, double price, String unit) {
        this.metal = metal;
        this.price = price;
        this.unit = unit;
    }

    public String getMetal() {
        return metal;
    }

    public double getPrice() {
        return price;
    }

    public String getUnit() {
        return unit;
    }
}
