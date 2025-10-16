package com.mazadak.orders.dto.client;

import java.util.List;
import java.util.UUID;

public record CartResponseDTO(
        UUID cartId,
        UUID userId,
        List<CartItemResponseDTO> cartItems
){ }

