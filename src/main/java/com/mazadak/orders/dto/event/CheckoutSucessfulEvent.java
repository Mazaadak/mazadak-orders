package com.mazadak.orders.dto.event;

import java.io.Serializable;
import java.util.UUID;

public record CheckoutSucessfulEvent(UUID orderId, UUID userId) implements Serializable {
}
