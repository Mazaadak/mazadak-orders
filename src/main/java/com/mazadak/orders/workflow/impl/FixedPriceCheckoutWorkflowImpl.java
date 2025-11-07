package com.mazadak.orders.workflow.impl;

import com.mazadak.orders.dto.internal.WorkflowResult;
import com.mazadak.orders.exception.CheckoutCancelledException;
import com.mazadak.orders.exception.CheckoutTimeoutException;
import com.mazadak.orders.model.enumeration.PaymentStatus;
import com.mazadak.orders.workflow.activity.CheckoutActivities;
import com.mazadak.orders.workflow.activity.FixedPriceCheckoutActivities;
import com.mazadak.orders.dto.client.CartResponseDTO;
import com.mazadak.orders.dto.request.CheckoutRequest;
import com.mazadak.orders.dto.response.OrderResponse;
import com.mazadak.orders.workflow.FixedPriceCheckoutWorkflow;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class FixedPriceCheckoutWorkflowImpl implements FixedPriceCheckoutWorkflow {
    private static final Logger log = Workflow.getLogger(AuctionCheckoutWorkflowImpl.class);
    private UUID currentOrderId;
    private String currentPaymentIntentId;
    private String clientSecret;
    private String cancellationReason;

    private boolean paymentAuthorized = false;
    private boolean checkoutCancelled = false;
    private boolean intentCreated = false;

    private final FixedPriceCheckoutActivities fixedPriceCheckoutActivities = Workflow.newActivityStub(
            FixedPriceCheckoutActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(2))
                            .setBackoffCoefficient(2.0)
                            .build()
                    ).build()
    );

    private final CheckoutActivities checkoutActivities = Workflow.newActivityStub(
            CheckoutActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(2))
                            .setBackoffCoefficient(2.0)
                            .build()
                    ).build()
    );

    @Override
    public WorkflowResult processCheckout(CheckoutRequest request, UUID idempotencyKey) {
        Saga saga = new Saga(new Saga.Options.Builder()
                .setParallelCompensation(false)
                .setContinueWithError(false)
                .build());

        OrderResponse order = null;

        try {
            // 1. Deactivate cart to prevent modifications
            fixedPriceCheckoutActivities.deactivateCart(request.userId());
            saga.addCompensation(() -> fixedPriceCheckoutActivities.activateCart(request.userId()));

            // 2. Get cart
            CartResponseDTO cart = fixedPriceCheckoutActivities.getCart(request.userId());

            // 3. Create Order
            order = fixedPriceCheckoutActivities.createOrder(request, cart, idempotencyKey);
            this.currentOrderId = order.id();
            // If we fail after this, we need to mark the order as failed
            saga.addCompensation(() -> checkoutActivities.markOrderAsFailed(currentOrderId));

            // 4. Assert amount does not exceed limit
            checkoutActivities.assertAmountNotTooLarge(order.id());

            // 5. Reserve inventory
            List<UUID> reservationIds = fixedPriceCheckoutActivities.reserveInventory(order.id(), cart.cartItems());
            // If we fail after this, we need to release the inventory reservations
            saga.addCompensation(
                    () -> fixedPriceCheckoutActivities.releaseInventoryReservations(currentOrderId, reservationIds)
            );

            // 6. Wait for intent creation
            boolean isIntentCreated = Workflow.await(
                    Duration.ofMinutes(15), // TODO make dynamic
                    () -> intentCreated || checkoutCancelled
            );

            if (checkoutCancelled) {
                throw new CheckoutCancelledException("Checkout cancelled during intent creation: " + cancellationReason);
            }

            if (!intentCreated) {
                throw new CheckoutTimeoutException("Intent not created within " + 15 + " minutes");
            }

            // 7. Associate payment intent and client secret with order
            checkoutActivities.setOrderPaymentIntentId(this.currentOrderId, currentPaymentIntentId);
            checkoutActivities.setOrderClientSecret(currentOrderId, clientSecret);
            log.info("Payment intent created and associated for order {}", currentOrderId);

            // 8. Wait for payment authorization
            boolean isPaymentAuthorized = Workflow.await(
                    Duration.ofMinutes(15), // TODO make dynamic
                    () -> paymentAuthorized || checkoutCancelled
            );

            if (checkoutCancelled) {
                throw new CheckoutCancelledException("Checkout cancelled during payment authorization");
            }

            if (!paymentAuthorized) {
                throw new CheckoutTimeoutException("Payment not authorized within " + 15 + " minutes"); // TODO make dynamic
            }

            log.info("Authorized payment for order {}", this.currentOrderId);

            checkoutActivities.setOrderPaymentStatus(this.currentOrderId, PaymentStatus.AUTHORIZED);

            // 9. If payment authorized, confirm reservation
            fixedPriceCheckoutActivities.confirmInventoryReservations(order.id(), reservationIds);

            // 10. Capture payment
            checkoutActivities.capturePayment(this.currentOrderId);
            saga.addCompensation(() -> checkoutActivities.refundPayment(this.currentOrderId));

            checkoutActivities.setOrderPaymentStatus(this.currentOrderId, PaymentStatus.CAPTURED);

            // 11. Clear and activate cart
            fixedPriceCheckoutActivities.clearCart(request.userId());
            fixedPriceCheckoutActivities.activateCart(request.userId());


            // 12. Send notifications
            checkoutActivities.sendCheckoutSuccessfulNotification(currentOrderId, order.buyerId(), order.totalAmount());

            // 13. Update order status to be completed
            checkoutActivities.markOrderAsCompleted(currentOrderId);

            return new WorkflowResult(true, "Checkout completed successfully", null);
        } catch (Exception e) {

            log.error("Error during checkout process", e);

            // Compensate for completed operations in reverse order
            Workflow.newDetachedCancellationScope(saga::compensate).run();

            // If we have an order ID but couldn't mark it as failed in the compensation
            if (order != null && order.id() != null) {
                try {
                    checkoutActivities.markOrderAsFailed(order.id());
                    checkoutActivities.cancelPaymentIntent(order.id());
                } catch (Exception ex) {
                    log.error("Failed to mark order as failed", ex);
                }
            }

            return new WorkflowResult(false, "Checkout failed", e.getMessage());
        }
    }

    @Override
    public void paymentAuthorized(UUID orderId, String paymentIntentId) {
        log.info("Signal received: paymentAuthorized for order: {}, intentId: {}", orderId, paymentIntentId);

        if (!orderId.equals(currentOrderId)) {
            log.warn("Ignoring payment authorization for non-current order: {} (current: {})", orderId, currentOrderId);
            return;
        }

        if (paymentAuthorized) {
            log.warn("Payment already authorized for order {}, ignoring duplicate", orderId);
            return;
        }

        if (checkoutCancelled) {
            log.warn("Checkout is cancelled for order {}, ignoring payment authorization", orderId);
            return;
        }

        this.currentPaymentIntentId = paymentIntentId;
        this.paymentAuthorized = true;

        log.info("Payment authorization accepted for order: {}", orderId);
    }

    @Override
    public void cancelCheckout(UUID orderId, String reason) {
        log.info("Signal received: cancelCheckout for order: {}, reason: {}", orderId, reason);

        if (!orderId.equals(currentOrderId)) {
            log.warn("Ignoring cancellation for non-current order: {} (current: {})", orderId, currentOrderId);
            return;
        }

        if (checkoutCancelled) {
            log.warn("Checkout already cancelled for order {}", orderId);
            return;
        }

        this.checkoutCancelled = true;
        this.cancellationReason = reason;

        log.info("Checkout cancellation accepted for order: {}, reason: {}", orderId, cancellationReason);
    }

    @Override
    public void intentCreated(UUID orderId, String paymentIntentId, String clientSecret) {
        log.info("Signal received: intentCreated for order: {}, intentId: {}", orderId, paymentIntentId);

        if (!orderId.equals(currentOrderId)) {
            log.warn("Ignoring intent creation for non-current order: {} (current: {})", orderId, currentOrderId);
            return;
        }

        if (intentCreated) {
            log.warn("Intent already created for order {}, ignoring duplicate", orderId);
            return;
        }

        if (checkoutCancelled) {
            log.warn("Checkout is cancelled for order {}, ignoring intent creation", orderId);
            return;
        }

        this.currentPaymentIntentId = paymentIntentId;
        this.clientSecret = clientSecret;
        this.intentCreated = true;

        log.info("Intent creation accepted for order: {}", orderId);
    }
}
