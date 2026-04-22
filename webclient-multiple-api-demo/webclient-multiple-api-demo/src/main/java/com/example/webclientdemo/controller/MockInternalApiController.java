package com.example.webclientdemo.controller;

import com.example.webclientdemo.dto.Employee;
import com.example.webclientdemo.dto.Order;
import com.example.webclientdemo.dto.Profile;
import com.example.webclientdemo.dto.Salary;
import com.example.webclientdemo.dto.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/mock")
public class MockInternalApiController {

    @GetMapping("/user/{id}")
    public Mono<User> getUser(@PathVariable Long id) {
        return Mono.just(new User(id, "Suresh", "suresh@test.com"))
                .delayElement(Duration.ofMillis(200));
    }

    @GetMapping("/orders/{userId}")
    public Mono<List<Order>> getOrders(@PathVariable Long userId) {
        return Mono.just(List.of(
                        new Order(101L, "Laptop", 75000.00),
                        new Order(102L, "Mouse", 1200.00),
                        new Order(103L, "Keyboard", 2500.00)
                ))
                .delayElement(Duration.ofMillis(300));
    }

    @GetMapping("/profile/{userId}")
    public Mono<Profile> getProfile(@PathVariable Long userId) {
        return Mono.just(new Profile(userId, "Detroit", "Gold"))
                .delayElement(Duration.ofMillis(150));
    }

    @GetMapping("/employee/{employeeId}")
    public Mono<Employee> getEmployee(@PathVariable Long employeeId) {
        return Mono.just(new Employee(employeeId, "EMP-" + employeeId, "Engineering"))
                .delayElement(Duration.ofMillis(100));
    }

    @GetMapping("/salary/{employeeCode}")
    public Mono<Salary> getSalary(@PathVariable String employeeCode) {
        return Mono.just(new Salary(9001L, 120000.00, 15000.00))
                .delayElement(Duration.ofMillis(250));
    }
}
