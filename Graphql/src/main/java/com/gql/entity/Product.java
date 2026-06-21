package com.gql.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Product {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String name;
	private String category;
	private Integer stock;
	private double price;
	
	public Product(String name, String category, Integer stock, double price) {
		super();
		this.name = name;
		this.category = category;
		this.stock = stock;
		this.price = price;
	}
	
	

}
