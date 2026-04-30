package com.goldexchange.fetcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RateFetcherApplication {
    public static void main(String[] args) {
        SpringApplication.run(RateFetcherApplication.class, args);
    }
}
