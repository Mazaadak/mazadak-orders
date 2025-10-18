package com.mazadak.orders.dto.response;

import java.util.UUID;

public record InventoryReservationResponse (
        UUID reservationId,
        UUID orderId,
        UUID productId,
        int quantity){
}
