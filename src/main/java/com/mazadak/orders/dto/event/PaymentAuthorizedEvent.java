package com.mazadak.orders.dto.event;

import java.math.BigDecimal;

public record PaymentAuthorizedEvent(
        String paymentIntentId,
        String orderId,
        String checkoutType,
        BigDecimal amount) {
}
