package com.example.repository;

import java.util.List;
import java.util.Optional;

import com.example.model.User;

public interface UserRepository {
	
	User save(User user);
	Optional<User> findById(int id);
	List<User> findAll();
	
	User update(User user);
	boolean deleteById(int id);
	List<User> findByName(String name);
}
