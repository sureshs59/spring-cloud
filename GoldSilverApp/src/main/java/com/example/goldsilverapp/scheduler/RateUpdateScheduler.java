package com.example.goldsilverapp.scheduler;

import com.example.goldsilverapp.dto.ExternalRateResponse;
import com.example.goldsilverapp.entity.MetalRateEntity;
import com.example.goldsilverapp.repository.MetalRateRepository;
import com.example.goldsilverapp.service.ExternalRateService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class RateUpdateScheduler {
    //private static final Logger log = LoggerFactory.getLogger(RateUpdateScheduler.class);
    private final ExternalRateService externalRateService;
    private final MetalRateRepository repository;

    public RateUpdateScheduler(
            ExternalRateService externalRateService, MetalRateRepository repository
    ) {
        this.externalRateService = externalRateService;

        this.repository = repository;
    }
    // Runs every day at 9:00 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void updateDailyRates(){
        saveTodayRates();
    }

    // Test scheduler every 1 minute
    // Uncomment for testing only
    // @Scheduled(fixedRate = 60000)
    public void updateEveryMinuteForTesting(){
        saveTodayRates();
    }

    private void saveTodayRates() {
        try {
            LocalDate today = LocalDate.now();

            List<ExternalRateResponse> rates = externalRateService.fetchTodayRates();

            for (ExternalRateResponse rate : rates) {

                MetalRateEntity entity = repository
                        .findByMetalAndRateDate(rate.getMetal(), today)
                        .orElse(new MetalRateEntity());

                entity.setMetal(rate.getMetal());
                entity.setRateDate(today);
                entity.setPrice(rate.getPrice());
                entity.setUnit(rate.getUnit());

                repository.save(entity);

                System.out.println("Saved/Updated rate: "
                        + rate.getMetal()
                        + " - "
                        + rate.getPrice());
            }

        } catch (Exception ex) {
            System.err.println("Failed to update rates: " + ex.getMessage());
        }

    }

}
