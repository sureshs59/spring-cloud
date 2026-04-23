package org.example.webclientresiliencedemo.controller;

import org.example.webclientresiliencedemo.model.Order;
import org.example.webclientresiliencedemo.model.Profile;
import org.example.webclientresiliencedemo.model.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
//This acts like your internal APIs so you can test everything in one app.
@RestController
@RequestMapping("/mock")
public class MockController {
    @GetMapping("/user/{id}")
    public User getUser(@PathVariable long id){
        return new User(id, "Suresh");
    }

    @GetMapping("/orders/{id}")
    public List<Order> getOrders(@PathVariable long id){
        return List.of(
                new Order(1L, "Laptop"),
                new Order(2L, "Computer")
        );
    }
    @GetMapping("/profile/{id}")
    public Profile getProfile(@PathVariable Long id) {
        return new Profile("Detroit");
    }

    @GetMapping("/profile-fail/{id}")
    public Profile getProfileFail(@PathVariable Long id) {
        throw new RuntimeException("Profile service is down");
    }
}
