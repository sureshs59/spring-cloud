package com.example.goldsilverapp.dto;

public class MlRateRecord {

    private String date;
    private double price;

    public MlRateRecord() {
    }

    public MlRateRecord(String date, double price) {
        this.date = date;
        this.price = price;
    }

    public String getDate() {
        return date;
    }

    public double getPrice() {
        return price;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
