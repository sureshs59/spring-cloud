package com.example.goldsilverapp.service;


import com.example.goldsilverapp.dto.MlPredictionRequest;
import com.example.goldsilverapp.dto.MlRateRecord;
import com.example.goldsilverapp.dto.PredictionResponse;
import com.example.goldsilverapp.entity.MetalRateEntity;
import com.example.goldsilverapp.repository.MetalRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class PredictionService {
    private final WebClient webClient;
    private final MetalRateRepository repository;

    public PredictionService(
            WebClient.Builder builder,
            MetalRateRepository repository
    ) {
        this.webClient = builder
                .baseUrl("http://localhost:8000")
                .build();

        this.repository = repository;
    }

    public PredictionResponse getPrediction(String metal) {

        List<MetalRateEntity> historyFromDb =
                repository.findByMetalOrderByRateDateAsc(metal);

        List<MlRateRecord> history = historyFromDb.stream()
                .map(rate -> new MlRateRecord(
                        rate.getRateDate().toString(),
                        rate.getPrice()
                ))
                .toList();

        MlPredictionRequest request = new MlPredictionRequest(metal, history);

        return webClient.post()
                .uri("/predict")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PredictionResponse.class)
                .block();
    }
}

