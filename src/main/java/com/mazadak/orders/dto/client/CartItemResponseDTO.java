package com.mazadak.orders.dto.client;


import java.util.UUID;

public record CartItemResponseDTO(
        UUID itemId,
        UUID productId,
        int quantity
) { }
