package org.example.webclientresiliencedemo.model;

import java.util.List;

public class DashboardResponse {
    public User user;
    public List<Order> orders;
    public Profile profile;

    public DashboardResponse() {
    }

    public DashboardResponse(User user, List<Order> orders, Profile profile) {
        this.user = user;
        this.orders = orders;
        this.profile = profile;
    }
}