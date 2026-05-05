package com.jwtdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JwtDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(JwtDemoApplication.class, args);
        System.out.println("""
                
                ╔══════════════════════════════════════════════════════╗
                ║           JWT Demo Backend Started!                  ║
                ║                                                      ║
                ║  POST /api/auth/login     → get tokens               ║
                ║  POST /api/auth/refresh   → refresh access token     ║
                ║  POST /api/auth/inspect   → decode any JWT           ║
                ║  GET  /api/dashboard      → protected endpoint       ║
                ║  GET  /api/admin/data     → admin only               ║
                ║  GET  /api/public/info    → no auth needed           ║
                ║                                                      ║
                ║  Users: admin/admin123  suresh/password              ║
                ╚══════════════════════════════════════════════════════╝
                """);
    }
}
