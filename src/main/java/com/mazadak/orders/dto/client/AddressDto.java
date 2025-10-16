package com.mazadak.orders.dto.client;

import jakarta.validation.constraints.*;

public record AddressDto(
        @NotBlank(message = "Street is mandatory")
        String street,
        @NotBlank(message = "City is mandatory")
        String city,
        @NotBlank(message = "State is mandatory")
        String state,
        @NotBlank(message = "Country is mandatory")
        String country,
        @NotBlank(message = "Postal code is mandatory")
        @Pattern(regexp = "^[0-9]{4,10}$",message = "Postal code must contain only digits")
        String postalCode
) {
}