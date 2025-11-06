package com.mazadak.orders.workflow.activity;

import com.mazadak.orders.model.entity.Address;
import com.mazadak.orders.model.enumeration.PaymentStatus;
import io.temporal.activity.ActivityInterface;

import java.util.UUID;

@ActivityInterface
public interface CheckoutActivities {
    void cancelOrder(UUID orderId);
    void setOrderAddress(UUID orderId, Address address);
    void setOrderPaymentIntentId(UUID orderId, String paymentIntentId);
    void capturePayment(UUID orderId);
    void refundPayment(UUID orderId);
    void cancelPaymentIntent(UUID orderId);
    void markOrderAsCompleted(UUID orderId);
    void markOrderAsFailed(UUID orderId);
    void setOrderPaymentStatus(UUID orderId, PaymentStatus status);
    String fetchUserEmail(UUID userId);
    void sendCheckoutSucessfulNotification(UUID orderId, UUID userId);

    void setOrderClientSecret(UUID currentOrderId, String clientSecret);
}
