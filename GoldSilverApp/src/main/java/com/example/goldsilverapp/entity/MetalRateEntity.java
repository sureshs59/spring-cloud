package com.example.goldsilverapp.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "metal_rates")
public class MetalRateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String metal;
    private LocalDate rateDate;
    private double price;
    private String unit;

    public MetalRateEntity() {
    }

    public MetalRateEntity(String metal, LocalDate rateDate, double price, String unit) {
        this.metal = metal;
        this.rateDate = rateDate;
        this.price = price;
        this.unit = unit;
    }

    public Long getId() {
        return id;
    }

    public String getMetal() {
        return metal;
    }

    public LocalDate getRateDate() {
        return rateDate;
    }

    public double getPrice() {
        return price;
    }

    public String getUnit() {
        return unit;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setMetal(String metal) {
        this.metal = metal;
    }

    public void setRateDate(LocalDate rateDate) {
        this.rateDate = rateDate;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
