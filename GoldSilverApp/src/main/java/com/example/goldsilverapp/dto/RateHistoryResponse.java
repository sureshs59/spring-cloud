package com.example.goldsilverapp.dto;

public class RateHistoryResponse {
    private String date;
    private double price;

    public RateHistoryResponse() {
    }

    public RateHistoryResponse(String date, double price) {
        this.date = date;
        this.price = price;
    }

    public String getDate() {
        return date;
    }

    public double getPrice() {
        return price;
    }
}
