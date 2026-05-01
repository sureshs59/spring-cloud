package com.example.goldsilverapp.dto;

public class MetalRateResponse {
    private String metal;
    private double todayRate;
    private double yesterdayRate;
    private String unit;
    private double change;
    private double changePercent;

    public MetalRateResponse() {
    }

    public MetalRateResponse(String metal, double todayRate, double yesterdayRate, String unit) {
        this.metal = metal;
        this.todayRate = todayRate;
        this.yesterdayRate = yesterdayRate;
        this.unit = unit;
        this.change = todayRate - yesterdayRate;

        if (yesterdayRate != 0) {
            this.changePercent = Math.round(((change / yesterdayRate) * 100) * 100.0) / 100.0;
        }
    }

    public String getMetal() {
        return metal;
    }

    public double getTodayRate() {
        return todayRate;
    }

    public double getYesterdayRate() {
        return yesterdayRate;
    }

    public String getUnit() {
        return unit;
    }

    public double getChange() {
        return change;
    }

    public double getChangePercent() {
        return changePercent;
    }
}
