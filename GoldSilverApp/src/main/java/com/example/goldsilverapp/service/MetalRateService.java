package com.example.goldsilverapp.service;

import com.example.goldsilverapp.dto.MetalRateResponse;
import com.example.goldsilverapp.dto.RateHistoryResponse;
import com.example.goldsilverapp.entity.MetalRateEntity;
import com.example.goldsilverapp.repository.MetalRateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class MetalRateService {
    private final MetalRateRepository repository;

    public MetalRateService(MetalRateRepository repository) {
        this.repository = repository;
    }

    public List<MetalRateResponse> getTodayRates() {
        return List.of(
                buildTodayRate("Gold"),
                buildTodayRate("Silver")
        );
    }

    private MetalRateResponse buildTodayRate(String metal) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        MetalRateEntity todayRate = repository.findByMetalAndRateDate(metal, today)
                .orElseThrow(() -> new RuntimeException("Today rate not found for " + metal));

        MetalRateEntity yesterdayRate = repository.findByMetalAndRateDate(metal, yesterday)
                .orElseThrow(() -> new RuntimeException("Yesterday rate not found for " + metal));

        return new MetalRateResponse(
                metal,
                todayRate.getPrice(),
                yesterdayRate.getPrice(),
                todayRate.getUnit()
        );
    }
    public List<RateHistoryResponse> getHistory(String metal) {
        return repository.findTop7ByMetalOrderByRateDateDesc(metal)
                .stream()
                .sorted(Comparator.comparing(MetalRateEntity::getRateDate))
                .map(rate -> new RateHistoryResponse(
                        rate.getRateDate().toString(),
                        rate.getPrice()
                ))
                .toList();
    }
}
