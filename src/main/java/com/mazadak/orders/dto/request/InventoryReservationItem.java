package com.mazadak.orders.dto.request;

import java.util.UUID;

public record InventoryReservationItem(
        UUID productId,
        Integer quantity
) {
}
