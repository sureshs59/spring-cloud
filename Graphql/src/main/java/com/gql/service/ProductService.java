package com.gql.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gql.entity.Product;
import com.gql.repository.ProductRepository;

@Service
public class ProductService {
	
	@Autowired
	private ProductRepository productRepository;
	
	public ProductRepository getProductRepository() {
		return productRepository;
	}
	
	public List<Product> getAllProducts() {
		return productRepository.findAll();
	}
	
	public List<Product> getProductByCategory(String category) {
		return  productRepository.findByCategory(category);
				//).elseThrow(() -> new RuntimeException("No products found in category: " + category));
	}
}
