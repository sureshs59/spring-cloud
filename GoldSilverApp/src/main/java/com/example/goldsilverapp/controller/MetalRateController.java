package com.example.goldsilverapp.controller;

import com.example.goldsilverapp.model.MetalRate;
import com.example.goldsilverapp.model.RateHistory;
import com.example.goldsilverapp.service.MetalRateService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rates")
@CrossOrigin(origins = "http://localhost:4200")
public class MetalRateController {

    private final MetalRateService service;

    public MetalRateController(MetalRateService service) {
        this.service = service;
    }

    @GetMapping("/today")
    public List<MetalRate> getTodayRates() {
        return service.getTodayRates();
    }

    @GetMapping("/history/gold")
    public List<RateHistory> getGoldHistory() {
        return service.getGoldHistory();
    }

    @GetMapping("/history/silver")
    public List<RateHistory> getSilverHistory() {
        return service.getSilverHistory();
    }
}