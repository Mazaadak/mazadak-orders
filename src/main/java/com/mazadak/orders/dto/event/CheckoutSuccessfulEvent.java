package com.mazadak.orders.dto.event;

import java.io.Serializable;
import java.util.UUID;

public record CheckoutSuccessfulEvent(UUID orderId, UUID userId) implements Serializable {
}
