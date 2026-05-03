package com.example.goldsilverapp.dto;

import java.util.List;

public class PredictionResponse {
    private String metal;
    private double latestPrice;
    private double predictedPrice;
    private String trend;
    private double changePercent;
    private double confidence;
    private String model;
    private List<String> featuresUsed;
    private String error;

    public PredictionResponse() {
    }

    public String getMetal() {
        return metal;
    }

    public double getLatestPrice() {
        return latestPrice;
    }

    public double getPredictedPrice() {
        return predictedPrice;
    }

    public String getTrend() {
        return trend;
    }

    public double getChangePercent() {
        return changePercent;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getModel() {
        return model;
    }

    public List<String> getFeaturesUsed() {
        return featuresUsed;
    }

    public String getError() {
        return error;
    }

    public void setMetal(String metal) {
        this.metal = metal;
    }

    public void setLatestPrice(double latestPrice) {
        this.latestPrice = latestPrice;
    }

    public void setPredictedPrice(double predictedPrice) {
        this.predictedPrice = predictedPrice;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public void setChangePercent(double changePercent) {
        this.changePercent = changePercent;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setFeaturesUsed(List<String> featuresUsed) {
        this.featuresUsed = featuresUsed;
    }

    public void setError(String error) {
        this.error = error;
    }
}
