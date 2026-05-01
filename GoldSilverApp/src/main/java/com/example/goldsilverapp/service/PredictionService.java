package com.example.goldsilverapp.service;


import com.example.goldsilverapp.dto.PredictionResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class PredictionService {
    private final WebClient webClient;

    public PredictionService(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("http://localhost:8000")
                .build();
    }

    public PredictionResponse getPrediction(String metal) {
        return webClient.get()
                .uri("/predict/{metal}", metal)
                .retrieve()
                .bodyToMono(PredictionResponse.class)
                .block();
    }
}

