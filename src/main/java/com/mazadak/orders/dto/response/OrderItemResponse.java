package com.mazadak.orders.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for {@link com.mazadak.orders.model.entity.OrderItem}
 */
public record OrderItemResponse(UUID id, UUID productId, String productName, String productImageUrl,
                                BigDecimal unitPrice, int quantity, BigDecimal subtotal) implements Serializable {
}