package com.example.springoauth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @GetMapping("/public")
    public String publicApi() {
        return "This is public API. No login required.";
    }

    @GetMapping("/user")
    public String userApi() {
        return "Welcome USER. You are authenticated.";
    }

    @GetMapping("/admin")
    public String adminApi() {
        return "Welcome ADMIN. You are authorized.";
    }
}