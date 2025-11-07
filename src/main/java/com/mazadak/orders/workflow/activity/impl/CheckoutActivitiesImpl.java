package com.mazadak.orders.workflow.activity.impl;

import com.mazadak.orders.client.PaymentClient;
import com.mazadak.orders.client.UserClient;
import com.mazadak.orders.dto.client.RefundRequest;
import com.mazadak.orders.dto.event.CheckoutSuccessfulEvent;
import com.mazadak.orders.exception.PaymentCaptureFailedException;
import com.mazadak.orders.model.entity.Address;
import com.mazadak.orders.model.enumeration.PaymentStatus;
import com.mazadak.orders.service.OrderService;
import com.mazadak.orders.workflow.activity.CheckoutActivities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class CheckoutActivitiesImpl implements CheckoutActivities {
    private final OrderService orderService;
    private final UserClient userClient;
    private final PaymentClient paymentClient;
    private final StreamBridge streamBridge;

    @Override
    public void cancelPaymentIntent(UUID orderId) {
        log.info("Cancelling intent for order {}", orderId);
        try {
            var response = paymentClient.cancelPayment(orderId).getBody();
            log.info("Cancelled intent {}", response.id());
        } catch (Exception e) {
            log.info("No intent was associated with order {}, thus cancellation was ignored", orderId);
        }
    }

    @Override
    public void cancelOrder(UUID orderId) {
        orderService.markCancelled(orderId);
    }

    @Override
    public void setOrderAddress(UUID orderId, Address address) {
        orderService.setAddress(orderId, address);
    }

    @Override
    public void setOrderPaymentIntentId(UUID orderId, String paymentIntentId) {
        orderService.setPaymentIntentId(orderId, paymentIntentId);
    }

    @Override
    public void capturePayment(UUID orderId) {
        log.info("Capturing payment for order {}", orderId);
        var response = paymentClient.capturePayment(orderId).getBody();
        if (response == null) {
            log.warn("Payment capture for order {} failed and response is null", orderId);
            throw new PaymentCaptureFailedException(orderId);
        }
        log.info("Payment captured successfully for order {}", orderId);
    }

    @Override
    public void refundPayment(UUID orderId) {
        log.info("Refunding payment for order {}", orderId);
        var response = paymentClient.refundPayment(new RefundRequest(orderId, orderId));
        log.info("Payment refunded successfully for order {}", orderId);
    }

    @Override
    public void markOrderAsCompleted(UUID orderId) {
        orderService.markCompleted(orderId);
    }

    @Override
    public void markOrderAsFailed(UUID orderId) {
        orderService.markFailed(orderId);
    }

    @Override
    public void setOrderPaymentStatus(UUID orderId, PaymentStatus status) {
        orderService.setPaymentStatus(orderId, status);
    }

    @Override
    public String fetchUserEmail(UUID userId) {
        return Objects.requireNonNull(userClient.getUser(userId, userId).getBody()) // TODO: attach header
                .email();
    }

    @Override
    public void sendCheckoutSuccessfulNotification(UUID orderId, UUID buyerId, BigDecimal amount) {
        log.info("Checkout successful for buyer {}. Order {}", buyerId, orderId);
        streamBridge.send("checkoutSuccessful-out-0", new CheckoutSuccessfulEvent(orderId, buyerId, amount));
    }

    @Override
    public void setOrderClientSecret(UUID currentOrderId, String clientSecret) {
        log.info("Setting client secret for order {}", currentOrderId);
        orderService.setClientSecret(currentOrderId, clientSecret);
    }
}
