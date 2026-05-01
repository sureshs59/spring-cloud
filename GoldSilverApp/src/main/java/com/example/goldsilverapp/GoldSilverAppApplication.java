package com.example.goldsilverapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GoldSilverAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoldSilverAppApplication.class, args);
    }

}
