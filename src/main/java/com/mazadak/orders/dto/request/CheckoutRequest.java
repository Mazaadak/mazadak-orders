package com.mazadak.orders.dto.request;

import com.mazadak.orders.model.entity.Address;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CheckoutRequest(
        @NotNull(message = "A checkout must be associated with a user") UUID userId,
        @NotNull(message = "User must choose a payment method to proceed with checkout") String paymentMethod,
        @NotNull(message = "User must enter address data to proceed with checkout") Address address
        ) {
}
