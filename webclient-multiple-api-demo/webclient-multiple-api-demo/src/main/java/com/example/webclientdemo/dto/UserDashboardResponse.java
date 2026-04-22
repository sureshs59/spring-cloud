package com.example.webclientdemo.dto;

import java.util.List;

public record UserDashboardResponse(User user, List<Order> orders, Profile profile) {
}
