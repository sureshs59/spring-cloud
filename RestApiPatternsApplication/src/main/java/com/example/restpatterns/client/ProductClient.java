package com.example.restpatterns.client;

import com.example.restpatterns.dto.Dtos;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "product-service", url = "${api.product-service}",
        fallback = FeignClients.ProductClientFallback.class)
public interface ProductClient {
    @GetMapping("/api/products/{id}")
    Dtos.ProductDTO getProductById(@PathVariable Long id);

    @GetMapping("/api/products")
    List<Dtos.ProductDTO> getAllProducts();

    @GetMapping("/api/products/batch")
    List<Dtos.ProductDTO> getBatch(@RequestParam String ids);
}
