package com.example.goldsilverapp.dto;

import java.util.List;

public class MlPredictionRequest {
    private String metal;
    private List<MlRateRecord> history;

    public MlPredictionRequest() {
    }

    public MlPredictionRequest(String metal, List<MlRateRecord> history) {
        this.metal = metal;
        this.history = history;
    }

    public String getMetal() {
        return metal;
    }

    public List<MlRateRecord> getHistory() {
        return history;
    }

    public void setMetal(String metal) {
        this.metal = metal;
    }

    public void setHistory(List<MlRateRecord> history) {
        this.history = history;
    }
}
