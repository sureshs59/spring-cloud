package com.example.actuatorDemo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long orderId;
	private String productName;
	private String category;
	private String color;
	private double price;
	
	public Order(String productName, String category, String color, double price) {
		this.productName = productName;
		this.category = category;
		this.color = color;
		this.price = price;
	}

	
}
