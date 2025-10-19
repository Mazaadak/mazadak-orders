package com.mazadak.orders.workflow.activity.impl;

import com.mazadak.orders.client.PaymentClient;
import com.mazadak.orders.client.UserClient;
import com.mazadak.orders.dto.client.RefundRequest;
import com.mazadak.orders.exception.PaymentCaptureFailedException;
import com.mazadak.orders.model.entity.Address;
import com.mazadak.orders.model.enumeration.PaymentStatus;
import com.mazadak.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class CheckoutActivitiesImpl implements CheckoutActivities {
    private final OrderService orderService;
    private final UserClient userClient;
    private final PaymentClient paymentClient;

    @Override
    public void cancelPaymentIntent(UUID orderId) {
        log.info("Cancelling intent for order {}", orderId);
        var response = paymentClient.cancelPayment(orderId).getBody();
        if (response == null) {
            log.info("No intent was associated with order {}, thus cancellation was ignored", orderId);
            return;
        }
        log.info("Cancelled intent {}", response.id());
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
}
