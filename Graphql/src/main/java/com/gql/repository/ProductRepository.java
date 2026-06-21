package com.gql.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gql.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {

	List<Product> findByCategory(String category);

}
