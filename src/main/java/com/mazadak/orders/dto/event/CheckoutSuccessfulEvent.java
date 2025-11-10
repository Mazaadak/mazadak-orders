package com.mazadak.orders.dto.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutSuccessfulEvent(UUID orderId, UUID buyerId, BigDecimal amount) implements Serializable {
}
