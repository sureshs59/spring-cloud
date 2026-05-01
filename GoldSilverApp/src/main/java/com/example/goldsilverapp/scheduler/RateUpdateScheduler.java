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
        LocalDate today = LocalDate.now();
        List<ExternalRateResponse> fetchTodayRates = externalRateService.fetchTodayRates();;

        for (ExternalRateResponse externalRateResponse : fetchTodayRates) {
//            boolean exists = repository.findByMetalAndRateDate(externalRateResponse.getMetal(), today)
//                    .isPresent();

            MetalRateEntity entity = repository
                    .findByMetalAndRateDate(externalRateResponse.getMetal(), today)
                    .orElse(new MetalRateEntity());

//            if(!exists){
//                MetalRateEntity entity = new MetalRateEntity();
                entity.setMetal(externalRateResponse.getMetal());
                entity.setRateDate(today);
                entity.setPrice(externalRateResponse.getPrice());
                entity.setUnit(externalRateResponse.getUnit());
                System.out.println(entity.getPrice()+"-Price updated for.."+entity.getMetal());
                repository.save(entity);
                System.out.println("Saved rate: "
                        + externalRateResponse.getMetal()
                        + " - "
                        + externalRateResponse.getPrice());
//            } else{
//                System.out.println("Rate already exists for "
//                        + externalRateResponse.getMetal()
//                        + " on "
//                        + today);
//            }

        }
    }

}
