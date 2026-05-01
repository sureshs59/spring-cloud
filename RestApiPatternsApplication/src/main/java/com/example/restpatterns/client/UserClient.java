package com.example.restpatterns.client;

import com.example.restpatterns.dto.Dtos;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "user-service", url = "${api.user-service}",
        fallback = FeignClients.UserClientFallback.class)
public interface UserClient {
    @GetMapping("/api/users/{id}")
    Dtos.UserDTO getUserById(@PathVariable Long id);

    @GetMapping("/api/users")
    List<Dtos.UserDTO> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size);

    @PostMapping("/api/users")
    Dtos.UserDTO createUser(@RequestBody Dtos.UserDTO user);

    @PutMapping("/api/users/{id}")
    Dtos.UserDTO updateUser(@PathVariable Long id, @RequestBody Dtos.UserDTO user);

    @DeleteMapping("/api/users/{id}")
    void deleteUser(@PathVariable Long id);

    @GetMapping("/api/users/search")
    List<Dtos.UserDTO> searchUsers(@RequestHeader("Authorization") String token,
                                   @RequestParam String keyword);
}
