package com.example.service;

import java.util.List;
import java.util.Optional;

import com.example.model.User;
import com.example.repository.UserRepository;

public class UserService {

	private final EmailService emailService;
	private final UserRepository userRepository;

	public UserService(EmailService emailService, UserRepository userRepository) {
		this.emailService = emailService;
		this.userRepository = userRepository;
	}
	
	public User registerUser(String name, String email) {
		if(name == null || name.isBlank()) {
			throw new IllegalArgumentException("Name cannot be null or empty");
		}
		if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
		
		User user = new User(0, name, email, true);
		User savedUser = userRepository.save(user);
		emailService.sendWelcomeEmail(savedUser.email());
		
		return savedUser;
	}
	
	 public User getUserById(int id) {
		Optional<User> user =  userRepository.findById(id);
		return user.orElse(null);
	 }
	 
	 public List<User> getAllUsers() {
	      return userRepository.findAll();
	 }
	 
	 public User updateUser(int id, String name, String email) {
		 Optional<User> existingUser =  userRepository.findById(id);
		 if(existingUser.isEmpty()) {
			 throw new IllegalArgumentException("User not found");
		 }
		 
		 User user = existingUser.get();
		 user = new User(user.id(), name != null ? name : user.name(), email != null ? email : user.email(), user.active());
		 
		 return userRepository.update(user);
	 }
	 
	 public boolean deactivateUser(int id) {
	        Optional<User> user = userRepository.findById(id);

	        if (user.isPresent()) {
	            User userToDeactivate = user.get();
	            userToDeactivate = new User(userToDeactivate.id(), userToDeactivate.name(), userToDeactivate.email(), false);
	            userRepository.update(userToDeactivate);
	            emailService.sendGoodbyeEmail(userToDeactivate.email());
	            return true;
	        }
	        return false;
	    }

	    public boolean deleteUser(int id) {
	        return userRepository.deleteById(id);
	    }

	    public List<User> searchUsersByName(String name) {
	        if (name == null || name.trim().isEmpty()) {
	            throw new IllegalArgumentException("Search name cannot be empty");
	        }
	        return userRepository.findByName(name);
	    }

	    public boolean activateUser(int id) {
	        Optional<User> user = userRepository.findById(id);

	        if (user.isPresent()) {
	            User userToActivate = user.get();
	            userToActivate.active();
	            userRepository.update(userToActivate);
	            return true;
	        }
	        return false;
	    }
	
}
