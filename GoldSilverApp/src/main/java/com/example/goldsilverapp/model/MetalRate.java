package com.example.goldsilverapp.model;

public class MetalRate {
    private String metal;
    private double todayRate;
    private double yesterdayRate;
    private String unit;
    private double change;
    private double changePercent;

    public MetalRate() {
    }

    public MetalRate(String metal, double todayRate, double yesterdayRate, String unit) {
        this.metal = metal;
        this.todayRate = todayRate;
        this.yesterdayRate = yesterdayRate;
        this.unit = unit;
        this.change = todayRate - yesterdayRate;
        this.changePercent = Math.round(((change / yesterdayRate) * 100) * 100.0) / 100.0;
    }

    public String getMetal() {
        return metal;
    }

    public void setMetal(String metal) {
        this.metal = metal;
    }

    public double getTodayRate() {
        return todayRate;
    }

    public void setTodayRate(double todayRate) {
        this.todayRate = todayRate;
    }

    public double getYesterdayRate() {
        return yesterdayRate;
    }

    public void setYesterdayRate(double yesterdayRate) {
        this.yesterdayRate = yesterdayRate;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getChange() {
        return change;
    }

    public void setChange(double change) {
        this.change = change;
    }

    public double getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(double changePercent) {
        this.changePercent = changePercent;
    }

}
