package com.mazadak.orders.workflow;

import java.util.UUID;

public interface CheckoutActivities {
    String createPaymentIntent(UUID orderId, UUID userId);
    void capturePayment(String paymentIntentId);
    void cancelPaymentIntent(String paymentIntentId);
    void markOrderAsCompleted(UUID orderId);
    void markOrderAsFailed(UUID orderId);
}
