package com.example.goldsilverapp.service;

import com.example.goldsilverapp.model.MetalRate;
import com.example.goldsilverapp.model.RateHistory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MetalRateService {
    public List<MetalRate> getTodayRates() {
        return List.of(
                new MetalRate("Gold", 72500, 71800, "10 gram"),
                new MetalRate("Silver", 83500, 84200, "1 kg")
        );
    }

    public List<RateHistory> getGoldHistory() {
        return List.of(
                new RateHistory("Apr 24", 70500),
                new RateHistory("Apr 25", 71000),
                new RateHistory("Apr 26", 71250),
                new RateHistory("Apr 27", 71800),
                new RateHistory("Apr 28", 72000),
                new RateHistory("Apr 29", 71800),
                new RateHistory("Apr 30", 72500)
        );
    }

    public List<RateHistory> getSilverHistory() {
        return List.of(
                new RateHistory("Apr 24", 82500),
                new RateHistory("Apr 25", 83000),
                new RateHistory("Apr 26", 83800),
                new RateHistory("Apr 27", 84500),
                new RateHistory("Apr 28", 84000),
                new RateHistory("Apr 29", 84200),
                new RateHistory("Apr 30", 83500)
        );
    }
}
