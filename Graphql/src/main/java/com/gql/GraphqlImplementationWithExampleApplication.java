package com.gql;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//import com.Java.Graphql.entity.Product;
//import com.Java.Graphql.repository.ProductRepository;

//import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class GraphqlImplementationWithExampleApplication {
	
//	@Autowired
//	private ProductRepository productRepository;
//	
//	@PostConstruct
//	public void initDB() {
//		
//		List<Product> products =  Stream.of( new Product("Laptop", "Electronics", 10, 999.99),
//		 new Product( "Smartphone", "Electronics", 20, 499.99),
//		 new Product( "Table", "Furniture", 5, 199.99),
//		 new Product( "Chair", "Furniture", 15, 89.99)).collect(java.util.stream.Collectors.toList());
//		
//		productRepository.saveAll(products);
//	}

	public static void main(String[] args) {
		SpringApplication.run(GraphqlImplementationWithExampleApplication.class, args);
	}

}
