package com.example.goldsilverapp.model;

public class RateHistory {
    private String date;
    private double price;

    public RateHistory() {
    }

    public RateHistory(String date, double price) {
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
