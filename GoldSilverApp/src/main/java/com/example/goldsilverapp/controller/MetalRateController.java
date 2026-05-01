package com.example.goldsilverapp.controller;

import com.example.goldsilverapp.dto.MetalRateResponse;
import com.example.goldsilverapp.dto.RateHistoryResponse;
import com.example.goldsilverapp.service.MetalRateService;
import org.springframework.web.bind.annotation.*;

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
    public List<MetalRateResponse> getTodayRates() {
        return service.getTodayRates();
    }

    @GetMapping("/history/{metal}")
    public List<RateHistoryResponse> getHistory(@PathVariable String metal) {
        return service.getHistory(metal);
    }
}