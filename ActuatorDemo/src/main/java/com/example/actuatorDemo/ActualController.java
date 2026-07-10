package com.example.actuatorDemo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/actual")
public class ActualController {
	
	@GetMapping("/test")
	public String getActual() {
		return "Actual Controller is working!";
	}

}
