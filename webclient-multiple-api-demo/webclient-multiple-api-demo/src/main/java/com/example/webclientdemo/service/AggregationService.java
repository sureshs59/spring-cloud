package com.example.webclientdemo.service;

import com.example.webclientdemo.dto.Employee;
import com.example.webclientdemo.dto.EmployeeSalaryResponse;
import com.example.webclientdemo.dto.Order;
import com.example.webclientdemo.dto.Profile;
import com.example.webclientdemo.dto.Salary;
import com.example.webclientdemo.dto.User;
import com.example.webclientdemo.dto.UserDashboardResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
public class AggregationService {

    private final WebClient webClient;
    private static final String BASE_URL = "http://localhost:8080/mock";

    public AggregationService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<UserDashboardResponse> getDashboardParallel(Long userId) {
        Mono<User> userMono = getUser(userId);
        Mono<List<Order>> ordersMono = getOrders(userId);
        Mono<Profile> profileMono = getProfile(userId);

        return Mono.zip(userMono, ordersMono, profileMono)
                .map(tuple -> new UserDashboardResponse(
                        tuple.getT1(),
                        tuple.getT2(),
                        tuple.getT3()))
                .doOnSubscribe(sub -> System.out.println("Started parallel dashboard calls"));
    }

    public Mono<EmployeeSalaryResponse> getEmployeeSalaryDependent(Long employeeId) {
        return getEmployee(employeeId)
                .flatMap(employee -> getSalary(employee.employeeCode())
                        .map(salary -> new EmployeeSalaryResponse(employee, salary)))
                .doOnSubscribe(sub -> System.out.println("Started dependent employee->salary flow"));
    }

    private Mono<User> getUser(Long userId) {
        return webClient.get()
                .uri(BASE_URL + "/user/{id}", userId)
                .retrieve()
                .bodyToMono(User.class)
                .timeout(Duration.ofSeconds(3));
    }

    private Mono<List<Order>> getOrders(Long userId) {
        return webClient.get()
                .uri(BASE_URL + "/orders/{userId}", userId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Order>>() {})
                .timeout(Duration.ofSeconds(3));
    }

    private Mono<Profile> getProfile(Long userId) {
        return webClient.get()
                .uri(BASE_URL + "/profile/{userId}", userId)
                .retrieve()
                .bodyToMono(Profile.class)
                .timeout(Duration.ofSeconds(3));
    }

    private Mono<Employee> getEmployee(Long employeeId) {
        return webClient.get()
                .uri(BASE_URL + "/employee/{employeeId}", employeeId)
                .retrieve()
                .bodyToMono(Employee.class)
                .timeout(Duration.ofSeconds(3));
    }

    private Mono<Salary> getSalary(String employeeCode) {
        return webClient.get()
                .uri(BASE_URL + "/salary/{employeeCode}", employeeCode)
                .retrieve()
                .bodyToMono(Salary.class)
                .timeout(Duration.ofSeconds(3));
    }
}
