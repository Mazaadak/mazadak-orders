package com.mazadak.orders.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponseDTO (
        UUID productId,
        UUID sellerId,
        String title,
        String description,
        BigDecimal price
){
}
