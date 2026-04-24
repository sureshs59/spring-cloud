package com.example.mulitpleapi.config;

//import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static reactor.netty.http.HttpConnectionLiveness.log;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                // Request logging interceptor
                .additionalInterceptors((request, body, execution) -> {
                    log.info("REST → {} {}", request.getMethod(), request.getURI());
                    ClientHttpResponse response = execution.execute(request, body);
                    log.info("REST ← status={}", response.getStatusCode());
                    return response;
                })
                .build();
    }
}
