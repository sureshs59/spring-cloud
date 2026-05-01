package com.example.restpatterns.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@Slf4j
public class AppConfig {

    @Value("${async.pool.core-size:10}")       private int coreSize;
    @Value("${async.pool.max-size:50}")        private int maxSize;
    @Value("${async.pool.queue-capacity:100}") private int queueCapacity;
    @Value("${async.pool.thread-prefix:api-}") private String threadPrefix;

    @Value("${api.user-service}")    private String userServiceUrl;
    @Value("${api.product-service}") private String productServiceUrl;
    @Value("${api.order-service}")   private String orderServiceUrl;
    @Value("${api.payment-service}") private String paymentServiceUrl;

    // ────────────────────────────────────────────────────────
    //  PATTERN 1 — RestTemplate
    // ────────────────────────────────────────────────────────

    /**
     * RestTemplate with timeouts and request/response logging.
     * Used by Pattern 1 (Sequential) and Pattern 5 (Resilience4j).
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {

        return builder.
                setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .additionalInterceptors( (request, body, execution) -> {
                    log.debug("[RestTemplate] --> {} {}", request.getMethod(), request.getURI());
                    var response = execution.execute(request, body);
                    log.debug("[RestTemplate] <-- {} {}", response.getStatusCode(), request.getURI());
                    return response;
        }).build();
    }
    // ────────────────────────────────────────────────────────
    //  PATTERN 2 — CompletableFuture thread pool
    // ────────────────────────────────────────────────────────

    /**
     * Dedicated thread pool for parallel API calls.
     * Never use the default ForkJoinPool for blocking I/O!
     */
    @Bean(name="apiExecutor")
    public Executor apiExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(coreSize);
        taskExecutor.setMaxPoolSize(maxSize);
        taskExecutor.setQueueCapacity(queueCapacity);
        taskExecutor.setThreadNamePrefix(threadPrefix);
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(30);
        taskExecutor.initialize();
        log.info("API executor pool: core={} max={} queue={}",
                coreSize, maxSize, queueCapacity);
        return taskExecutor;
    }
    // ────────────────────────────────────────────────────────
    //  PATTERN 3 — WebClient (one per downstream service)
    // ────────────────────────────────────────────────────────
    @Bean(name="userWebClient")
    public WebClient userWebClient(WebClient.Builder builder) {
        return buildWebClient(builder, userServiceUrl, "UserSvc");
    }

    @Bean(name="productWebClient")
    public WebClient productWebClient(WebClient.Builder builder) {
        return buildWebClient(builder, productServiceUrl, "ProductSvc");
    }

    @Bean(name = "orderWebClient")
    public WebClient orderWebClient(WebClient.Builder builder) {
        return buildWebClient(builder, orderServiceUrl, "OrderSvc");
    }

    @Bean("paymentWebClient")
    public WebClient paymentWebClient(WebClient.Builder builder) {
        return buildWebClient(builder, paymentServiceUrl, "PaymentSvc");
    }

    /**
     * Shared WebClient builder with:
     *  - Connection timeout:  5 seconds
     *  - Response timeout:   10 seconds
     *  - Request/response logging filter
     *  - JSON content type header
     */
    private WebClient buildWebClient(WebClient.Builder builder, String url, String clientName) {
        HttpClient httpclient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(5))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(10))
                            .addHandlerLast(new WriteTimeoutHandler(5)));

        return builder.baseUrl(url).clientConnector(new ReactorClientHttpConnector(httpclient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest(clientName))
                .filter(logResponse(clientName))
                .build();

    }

    private ExchangeFilterFunction logRequest(String name) {
        return ExchangeFilterFunction.ofRequestProcessor(req -> {
            log.debug("[{}] --> {} {}", name, req.method(), req.url());
            return Mono.just(req);
        });
    }

    private ExchangeFilterFunction logResponse(String name) {
        return ExchangeFilterFunction.ofResponseProcessor(resp -> {
            log.debug("[{}] <-- {}", name, resp.statusCode());
            return Mono.just(resp);
        });
    }
}
