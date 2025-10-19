package com.mazadak.orders.dto.client;

import java.util.UUID;

public record RefundRequest(
        UUID orderId,
        UUID idempotencyKey
) {
}
