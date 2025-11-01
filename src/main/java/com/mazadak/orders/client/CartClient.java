package com.mazadak.orders.client;

import com.mazadak.orders.dto.client.CartResponseDTO;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "cart-service")
public interface CartClient {
    @GetMapping("/carts")
    ResponseEntity<CartResponseDTO> getCart(@RequestHeader("X-User-Id") @NotNull(message = "User ID is required") UUID userId);

    @PostMapping("/carts/deactivate")
    ResponseEntity<Void> deactivateCart(@RequestHeader("X-User-Id") @NotNull(message = "User ID is required") UUID userId);

    @PostMapping("/carts/activate")
    ResponseEntity<Void> activateCart(@RequestHeader("X-User-Id") @NotNull(message = "User ID is required") UUID userId);

    @PostMapping("/carts/clear")
    ResponseEntity<Void> clearCart(@RequestHeader("X-User-Id") @NotNull(message = "User ID is required") UUID userId);
}
