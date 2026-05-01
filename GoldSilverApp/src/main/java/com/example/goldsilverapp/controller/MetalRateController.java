package com.example.goldsilverapp.controller;

import com.example.goldsilverapp.dto.MetalRateResponse;
import com.example.goldsilverapp.dto.PredictionResponse;
import com.example.goldsilverapp.dto.RateHistoryResponse;
import com.example.goldsilverapp.scheduler.RateUpdateScheduler;
import com.example.goldsilverapp.service.MetalRateService;
import com.example.goldsilverapp.service.PredictionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rates")
@CrossOrigin(origins = "http://localhost:4200")
public class MetalRateController {

    private final MetalRateService service;
    private final RateUpdateScheduler scheduler;
    private final PredictionService predictionService;

    public MetalRateController(
            MetalRateService service,
            RateUpdateScheduler scheduler,
            PredictionService predictionService
    ) {
        this.service = service;
        this.scheduler = scheduler;
        this.predictionService = predictionService;
    }

    @GetMapping("/today")
    public List<MetalRateResponse> getTodayRates() {
        return service.getTodayRates();
    }

    @GetMapping("/history/{metal}")
    public List<RateHistoryResponse> getHistory(@PathVariable String metal) {
        return service.getHistory(metal);
    }

    @PostMapping("/update-now")
    public String updateNow(){
        scheduler.updateEveryMinuteForTesting();
        return "Rates update triggered successfully";
    }

    @GetMapping("/prediction/{metal}")
    public PredictionResponse getPrediction(@PathVariable String metal) {
        return predictionService.getPrediction(metal);
    }
}