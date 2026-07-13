package com.example.actuatorDemo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class WebConfig {
	
//	@Autowired
//	private RestClient restClient;
	
	@Bean
	public RestClient getRestClient() {
		System.out.println("RestClient bean created *******************************************");
		return  RestClient.create("http://localhost:8080/users");
	}

}
