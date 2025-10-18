package com.mazadak.orders.workflow;

import com.mazadak.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class CheckoutActivitiesImpl implements CheckoutActivities {
    protected final OrderService orderService;

    @Override
    public String createPaymentIntent(UUID orderId, UUID userId) {
        log.info("Creating payment intent for order {} and associated with user {}", orderId, userId);
        return "pi123";
        // TODO: implement
    }

    @Override
    public void capturePayment(String paymentIntentId) {
        log.info("Capturing intent {}", paymentIntentId);
        // TODO: implement
    }

    @Override
    public void cancelPaymentIntent(String paymentIntentId) {
        log.info("Cancelling intent {}", paymentIntentId);
        // TODO: implement
    }

    @Override
    public void markOrderAsCompleted(UUID orderId) {
        orderService.markCompleted(orderId);
    }

    @Override
    public void markOrderAsFailed(UUID orderId) {
        orderService.markFailed(orderId);
    }
}
