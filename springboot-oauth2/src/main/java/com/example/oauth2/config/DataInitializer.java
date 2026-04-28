package com.example.oauth2.config;

import com.example.oauth2.model.AuthProvider;
import com.example.oauth2.model.Role;
import com.example.oauth2.model.User;
import com.example.oauth2.service.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds test data into the H2 database on application startup.
 *
 * Creates two users so you can test without registering:
 *   Admin:  admin@example.com  / admin123  (ROLE_ADMIN)
 *   User:   user@example.com   / user123   (ROLE_USER)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (!userRepository.existsByEmail("admin@example.com")) {
            userRepository.save(User.builder()
                    .name("Admin User")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("admin123"))
                    .provider(AuthProvider.local)
                    .emailVerified(true)
                    .role(Role.ROLE_ADMIN)
                    .build());
            log.info("Seeded admin user:  admin@example.com / admin123");
        }

        if (!userRepository.existsByEmail("user@example.com")) {
            userRepository.save(User.builder()
                    .name("Test User")
                    .email("user@example.com")
                    .password(passwordEncoder.encode("user123"))
                    .provider(AuthProvider.local)
                    .emailVerified(true)
                    .role(Role.ROLE_USER)
                    .build());
            log.info("Seeded regular user: user@example.com / user123");
        }
    }
}
