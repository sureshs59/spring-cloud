package com.gql.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


import com.gql.entity.Product;
import com.gql.service.ProductService;

// Changing from @RestController to @Controller to avoid conflict with GraphQL endpoint
//@RestController  
//@RequestMapping("/products")
@Controller
public class ProductController {

	@Autowired
	private ProductService productService;
	
	@QueryMapping
	public List<Product> getAllProducts() {
		return productService.getAllProducts();
	}
	
	@QueryMapping
	public List<Product> getProductByCategory(@Argument String category) {
		return  productService.getProductByCategory(category);
				//).elseThrow(() -> new RuntimeException("No products found in category: " + category));
	}
	
	//Save using GraphQL Mutation
	@MutationMapping
	public Product updateStock(@Argument Integer id, @Argument Integer stock) {
		return productService.updateStock(id, stock);
	}
	
	@MutationMapping
	public Product receivedNewPrice(@Argument Integer id, @Argument double price) {
		return productService.receivedNewPrice(id, price);
	}
	
	@MutationMapping
	public Product saveProduct(@Argument String name, @Argument String category, @Argument Integer stock, @Argument double price) {
		Product newProduct = new Product(name, category, stock, price);
		return productService.getProductRepository().save(newProduct);
	}
}
