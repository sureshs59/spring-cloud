package com.example.goldsilverapp.service;

import com.example.goldsilverapp.dto.ExternalRateResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class ExternalRateService {

    private final WebClient webClient;

    public ExternalRateService(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://api.example.com")
                .build();
    }

    public List<ExternalRateResponse> fetchTodayRates() {
        return webClient.get()
                .uri("/rates")
                .retrieve()
                .bodyToFlux(ExternalRateResponse.class)
                .collectList()
                .block();
    }
}