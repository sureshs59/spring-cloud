package org.example.webclientresiliencedemo.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.example.webclientresiliencedemo.model.DashboardResponse;
import org.example.webclientresiliencedemo.model.Order;
import org.example.webclientresiliencedemo.model.Profile;
import org.example.webclientresiliencedemo.model.User;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
public class AggregationService {
    private  final WebClient webClient;

    public AggregationService(WebClient webClient) {
        this.webClient = webClient;
    }
    public Mono<DashboardResponse> getParallel(Long id){
        Mono<User> getUsers = getUser(id);
        Mono<Profile> listProfile = getProfile(id);
        Mono<List<Order>> listOrders = getOrder(id);
        System.out.println("getParallel Service called...");

        return Mono.zip(getUsers, listOrders, listProfile)
                .map(tp -> new DashboardResponse(
                        tp.getT1(),
                        tp.getT2(),
                        tp.getT3()
                ));
    }

    public Mono<DashboardResponse> getDependent(Long id){
        System.out.println("getDependent Service called...");
        return getUser(id)
                .flatMap(user -> Mono.zip(
                        getOrder(user.id),
                        getProfile(user.id)
                ).map(tuple -> new DashboardResponse(
                        user,
                        tuple.getT1(),
                        tuple.getT2()
                )));
    }

    @Retry(name= "myService", fallbackMethod = "usersFallback")
    private Mono<User> getUser(Long id){
        System.out.println("getUser() method called in service...");
        return webClient.get().uri("/mock/user/{id}", id)
                .retrieve()
                .bodyToMono(User.class)
                .timeout(Duration.ofSeconds(3));
    }
    @Retry(name="myService", fallbackMethod = "ordersFallback")
    private Mono<List<Order>> getOrder(Long id){
        System.out.println("getOrder() method called in service...");
        return webClient.get().uri("/mock/orders/{id}", id)
                .retrieve()
                .bodyToFlux(Order.class)
                .collectList()
                .timeout(Duration.ofSeconds(3));
    }

    @CircuitBreaker(name="myService", fallbackMethod = "profileFallback")
    @Retry(name="myService", fallbackMethod = "profileFallback")
    private Mono<Profile> getProfile(Long id){
        System.out.println("getProfile() method called in service...");
        return webClient.get().uri("/mock/profile/{id}", id)
                .retrieve()
                .bodyToMono(Profile.class)
                .timeout(Duration.ofSeconds(3));
    }

    @CircuitBreaker(name="myService", fallbackMethod = "profileFallback")
    @Retry(name="myService", fallbackMethod = "profileFallback")
    public Mono<Profile> getProfileFailing(Long id){
        System.out.println("getProfileFailing Service called...");
        return webClient.get().uri("/mock/profile-fail/{id}", id)
                .retrieve()
                .bodyToMono(Profile.class)
                .timeout(Duration.ofSeconds(3));
    }

    private Mono<User> usersFallback(Long id, Throwable ex){
        return Mono.just(new User(id, "System User"));
    }
    private Mono<List<Order>> ordersFallback(Long id, Throwable ex){
        return Mono.just(List.of(new Order(0l, "Default Order")));
    }
    private Mono<Profile> profileFallback(Long id, Throwable ex){
        return Mono.just(new Profile( "Default City Profile"));
    }
}
