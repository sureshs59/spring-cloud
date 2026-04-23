package org.example.webclientresiliencedemo.controller;

import org.example.webclientresiliencedemo.model.DashboardResponse;
import org.example.webclientresiliencedemo.model.Profile;
import org.example.webclientresiliencedemo.service.AggregationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class MainController {
    private final AggregationService aggregationService;

    public MainController(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }
    @GetMapping("/parallel/{id}")
    public Mono<DashboardResponse> parallel(@PathVariable long id) {
        System.out.println("parallel called...");
        return aggregationService.getParallel(id);
    }

    @GetMapping("/dependent/{id}")
    public Mono<DashboardResponse> dependent(@PathVariable long id) {
        System.out.println("dependent called...");
        return aggregationService.getDependent(id);
    }
    @GetMapping("/profile-fallback/{id}")
    public Mono<Profile> profileFallback(@PathVariable Long id) {
        System.out.println("profile fallback called...");
        return aggregationService.getProfileFailing(id);
    }
}
