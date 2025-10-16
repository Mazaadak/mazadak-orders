package com.mazadak.orders.client;

import com.mazadak.orders.dto.client.CartResponseDTO;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "cart-service")
public interface CartClient {
    @GetMapping("/carts")
    ResponseEntity<CartResponseDTO> getCart(@RequestHeader("user-id") @NotNull(message = "User ID is required") UUID userId);
}
