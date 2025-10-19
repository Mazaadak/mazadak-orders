package com.mazadak.orders.dto.client;

import java.io.Serializable;

public record PaymentIntentResponse(
        String id,
        String status,
        Long amount,
        String currency
) implements Serializable {
}
