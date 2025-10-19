package com.mazadak.orders.dto.client;

import java.io.Serializable;
import java.util.UUID;

public record RefundResponse(
        String refundId,
        UUID orderId,
        String status,
        String message
) implements Serializable {
}
