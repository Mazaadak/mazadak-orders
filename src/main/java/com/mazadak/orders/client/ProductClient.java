package com.mazadak.orders.client;

import com.mazadak.orders.dto.response.ProductResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "product-catalog")
public interface ProductClient {
    @PostMapping("/products/batch")
     ResponseEntity<List<ProductResponseDTO>> getProductsByIds(@RequestBody List<UUID> productIds);
}
