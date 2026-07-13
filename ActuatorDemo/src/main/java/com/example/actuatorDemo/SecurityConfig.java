package com.example.actuatorDemo;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity httpSecurity)throws Exception {
		
		httpSecurity.csrf(Customizer.withDefaults()).authorizeHttpRequests(
				auth -> 
				 // Allow anyone to check the basic health end point
				auth.requestMatchers("/actuator/health").permitAll()
				// Protect all other actuator end points; require ADMIN role
				.requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("ADMIN")
				.anyRequest().authenticated()
				).httpBasic(Customizer.withDefaults());  // Use basic HTTP authentication
		return httpSecurity.build();
	}
	
	@Bean
	public UserDetailsService userDetailsService(PasswordEncoder encoder) {
		// Create an in-memory user with the ADMIN role for testing
		UserDetails admin = User.builder()
							.username("admin")
							.password(encoder.encode("admin123"))
							.roles("ADMIN")
							.build();
				
		UserDetails user = User.builder()
	                .username("suresh")
	                .password(encoder.encode("admin123"))
	                .roles("USER")
	                .build();
		return new InMemoryUserDetailsManager(admin, user);
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
