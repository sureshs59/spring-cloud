package com.example.webclientdemo.controller;

import com.example.webclientdemo.dto.EmployeeSalaryResponse;
import com.example.webclientdemo.dto.UserDashboardResponse;
import com.example.webclientdemo.service.AggregationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class AggregationController {

    private final AggregationService aggregationService;

    public AggregationController(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @GetMapping("/dashboard-parallel/{userId}")
    public Mono<UserDashboardResponse> getDashboardParallel(@PathVariable Long userId) {
        return aggregationService.getDashboardParallel(userId);
    }

    @GetMapping("/employee-salary-dependent/{employeeId}")
    public Mono<EmployeeSalaryResponse> getEmployeeSalaryDependent(@PathVariable Long employeeId) {
        return aggregationService.getEmployeeSalaryDependent(employeeId);
    }
}
