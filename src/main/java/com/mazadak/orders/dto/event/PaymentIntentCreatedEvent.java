package com.mazadak.orders.dto.event;

public record PaymentIntentCreatedEvent(String paymentIntentId, String clientSecret, String orderId) {
}
