package com.mazadak.orders.client;

import com.mazadak.orders.dto.request.ConfirmReservationRequest;
import com.mazadak.orders.dto.request.ReleaseReservationRequest;
import com.mazadak.orders.dto.request.ReserveInventoryRequest;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @PostMapping("/inventories/reservations")
    ResponseEntity<List<UUID>> reserveInventory(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestBody ReserveInventoryRequest request
    );

    @PostMapping("/inventories/reservations/confirm")
    void confirmInventoryReservations(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestBody ConfirmReservationRequest request
    );

    @PostMapping("/inventories/reservations/release")
    public ResponseEntity<Void> releaseReservation(
            @RequestHeader("Idempotency-Key") @NotNull UUID idempotencyKey,
            @NotNull @RequestBody List<UUID> reservationIds);
}
