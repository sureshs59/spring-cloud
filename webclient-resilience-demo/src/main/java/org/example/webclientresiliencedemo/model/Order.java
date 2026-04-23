package org.example.webclientresiliencedemo.model;

public class Order {
    public Long orderId;
    public String product;

    public Order() {
    }

    public Order(Long orderId, String product) {
        this.orderId = orderId;
        this.product = product;
    }
}