package com.example.goldsilverapp.service;

import org.springframework.web.reactive.function.client.WebClient;

public class ExternalRateServiceBuilder {
    private WebClient.Builder builder;

    public ExternalRateServiceBuilder setBuilder(WebClient.Builder builder) {
        this.builder = builder;
        return this;
    }

    public ExternalRateService createExternalRateService() {
        return new ExternalRateService(builder);
    }
}