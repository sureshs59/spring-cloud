package com.example.actuatorDemo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@RestController
@RequestMapping("/orders")
public class ActualController {
	
	@Autowired
	private OrderRepository orderRepository;
	
	@Autowired
	private RestClient restClient;
	
	public ActualController(OrderRepository orderRepository, RestClient restClient) {
		this.orderRepository = orderRepository;
		this.restClient = restClient;
	}
	
	
//	@PostConstruct
//	public void initOrdersTable() {
//		// Initialize the orders table with some sample data
//		System.out.println("Loading orders for category: @PostConstruct  ");
//		orderRepository.saveAll(Stream.of(
//				new Order("Mobile", "electronics", "white", 100.0),
//				new Order("Laptop", "electronics", "black", 500.0),
//				new Order( "Shirt", "clothing", "blue", 20.0)
//				)
//				.collect(Collectors.toList()));
//		
//	}
	@GetMapping("/all")
	public List<Order> getAllOrders() {
		// Fetch all orders
		System.out.println("Fetching all orders");
		return orderRepository.findAll();
	}
	
	
	@GetMapping("/{category}")
	@CircuitBreaker(name = "orderService", fallbackMethod = "fallbackGetOrderByCategory")
	public List<Order> getOrderByCategory(@PathVariable String category) {
			// Fetch orders by category
		System.out.println("Fetching orders for category: " + category);
		List<Order> orders =null;
		if (category == null || category.isEmpty()) {
			throw new IllegalArgumentException("Category must not be null or empty");
		} else {
			System.out.println("Fetching orders for category: " + category);
			//orders = orderRepository.findByCategory(category);
			this.restClient.get().uri("/info").retrieve().body(String.class).toString();
		}
		return orders;
	}

	public List<Order> fallbackGetOrderByCategory(Exception ex) {
		System.out.println("Fallback method called for category: " + ". Reason: " + ex.getMessage());
		return List.of(new Order("MyComputer","Lenovo","silver",500)); // Return an empty list as a fallback
	}
}
