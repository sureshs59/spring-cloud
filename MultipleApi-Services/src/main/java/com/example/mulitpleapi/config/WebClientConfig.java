package com.example.mulitpleapi.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import reactor.netty.http.client.HttpClient;
import java.time.Duration;

import static reactor.netty.http.HttpConnectionLiveness.log;

@Configuration
public class WebClientConfig {

    @Bean("userWebClient")
    public WebClient userWebClient(
            @Value("${api.user-service}") String baseUrl) {
        return buildClient(baseUrl);
    }

    @Bean("productWebClient")
    public WebClient productWebClient(
            @Value("${api.product-service}") String baseUrl) {
        return buildClient(baseUrl);
    }

    @Bean("orderWebClient")
    public WebClient orderWebClient(
            @Value("${api.order-service}") String baseUrl) {
        return buildClient(baseUrl);
    }

    private WebClient buildClient(String baseUrl) {
        // Connection pool settings
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(10))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(10))
                                .addHandlerLast(new WriteTimeoutHandler(5)));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE)
                // Request/response logging filter
                .filter(ExchangeFilterFunction.ofRequestProcessor(req -> {
                    log.info("WC → {} {}", req.method(), req.url());
                    return Mono.just(req);
                }))
                .build();
    }
}
