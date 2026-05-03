package com.example.goldsilverapp.service;

import com.example.goldsilverapp.config.MetalApiProperties;
import com.example.goldsilverapp.dto.ExternalRateResponse;
import com.example.goldsilverapp.dto.MetalApiResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class ExternalRateService {

    private final WebClient webClient;
    private final MetalApiProperties properties;

    public ExternalRateService(
            WebClient.Builder builder,
            MetalApiProperties properties
    ) {
        this.properties = properties;
        this.webClient = builder
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    public List<ExternalRateResponse> fetchTodayRates() {

        MetalApiResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/2026-05-01")
                        .queryParam("api_key", properties.getKey())
                        .queryParam("base", "INR")
                        .queryParam("currencies", "XAU,XAG")
                        .build())
                .retrieve()
                .bodyToMono(MetalApiResponse.class)
                .block();
//       String rawResponse = webClient.get()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/latest")
//                        .queryParam("api_key", properties.getKey())
//                        .queryParam("base", "INR")
//                        .queryParam("currencies", "XAU,XAG")
//                        .build())
//                .retrieve()
//                .bodyToMono(String.class)
//                .block();
//        System.out.println(rawResponse);

        if (response == null || response.getRates() == null) {
            throw new RuntimeException("Failed to fetch metal rates from external API");
        }

        Double goldRate = response.getRates().get("INRXAU");
        Double silverRate = response.getRates().get("INRXAG");
        System.out.println("response.getRates() Gold Rate-->"+goldRate);
        System.out.println("response.getRates() silver Rate-->"+silverRate);
        return List.of(
                new ExternalRateResponse("Gold", convertGoldRate(goldRate), "10 gram"),
                new ExternalRateResponse("Silver", convertSilverRate(silverRate), "1 kg")
        );
    }

    private double convertGoldRate(Double apiValue) {
        if (apiValue == null) {
            throw new RuntimeException("Gold rate missing from API response");
        }

        /*
         IMPORTANT:
         API providers return rates differently.
         Some return XAU per currency, some return currency per troy ounce.
         Verify provider documentation before using this conversion in production.
        */

        return goldPer10Gram(apiValue);
    }

    private double convertSilverRate(Double apiValue) {
        if (apiValue == null) {
            throw new RuntimeException("Silver rate missing from API response");
        }

        return silverPerKg(apiValue);
    }
    private double goldPer10Gram(double inrPerTroyOunce) {
        return Math.round((inrPerTroyOunce / 31.1034768 * 10) * 100.0) / 100.0;
    }

    private double silverPerKg(double inrPerTroyOunce) {
        return Math.round((inrPerTroyOunce / 31.1034768 * 1000) * 100.0) / 100.0;
    }
}