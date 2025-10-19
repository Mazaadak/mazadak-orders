package com.mazadak.orders.workflow.impl;

import com.mazadak.orders.exception.CheckoutCancelledException;
import com.mazadak.orders.exception.CheckoutTimeoutException;
import com.mazadak.orders.model.entity.Address;
import com.mazadak.orders.model.enumeration.PaymentStatus;
import com.mazadak.orders.dto.internal.AuctionCheckoutRequest;
import com.mazadak.orders.workflow.AuctionCheckoutWorkflow;
import com.mazadak.orders.workflow.activity.impl.AuctionCheckoutActivities;
import com.mazadak.orders.workflow.activity.impl.CheckoutActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.UUID;

public class AuctionCheckoutWorkflowImpl implements AuctionCheckoutWorkflow {
    private static final Logger log = Workflow.getLogger(AuctionCheckoutWorkflowImpl.class);
    private final AuctionCheckoutActivities auctionActivities = Workflow.newActivityStub(
            AuctionCheckoutActivities.class,
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
                    .setStartToCloseTimeout(Duration.ofSeconds(45))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(2))
                            .setBackoffCoefficient(2.0)
                            .build()
                    ).build()
    );

    private UUID currentOrderId;
    private String currentPaymentIntentId;
    private Address shippingAddress;

    private boolean addressProvided = false;
    private boolean paymentAuthorized = false;
    private boolean checkoutCancelled = false;
    private String cancellationReason;

    public void processAuctionCheckout(AuctionCheckoutRequest request) {
        log.info("Starting auction workflow for auction {}", request.auction().id());

        for (var bidder : request.bidders()) {
            if (processBidderCheckout(request, bidder)) {
                log.info("Auction checkout completed successfully with bidder {}", bidder.id());
                return;
            }
        }

        log.error("All bidders failed checkout for auction {}", request.auction().id());
        auctionActivities.emitAuctionInvalidEvent(request.auction().id());
    }

    public boolean processBidderCheckout(AuctionCheckoutRequest request, AuctionCheckoutRequest.BidderInfo bidder) {
        resetWorkflowState();

        Saga.Options sagaOptions = new Saga.Options.Builder()
                .setParallelCompensation(false)
                .build();

        Saga saga = new Saga(sagaOptions);

        try {
            log.info("Starting checkout for bidder {}", bidder.id());

            // STEP 1: create and persist order record for this bidder
            currentOrderId = auctionActivities.createOrderForWinner(
                    request.auction(),
                    bidder
            );
            saga.addCompensation(() -> checkoutActivities.cancelOrder(currentOrderId));
            log.info("Created order: {} for bidder {}", currentOrderId, bidder.id());

            // STEP 1.5: fetch user email
            var email = checkoutActivities.fetchUserEmail(bidder.id());

            // STEP 2: notify winner to start checkout (sends email with checkout url)
            auctionActivities.notifyWinnerToCheckout(bidder, email, currentOrderId);
            saga.addCompensation(() -> auctionActivities.notifyWinnerCheckoutFailed(bidder, email, currentOrderId));

            // STEP 3: wait for shipping address
            boolean receivedAddress = Workflow.await(
                    Duration.ofHours(24), // TODO make dynamic
                    () -> addressProvided || checkoutCancelled
            );

            if (checkoutCancelled) {
                throw new CheckoutCancelledException("Checkout cancelled during address submission: " + cancellationReason);
            }

            if (!receivedAddress) {
                throw new CheckoutTimeoutException("Address not provided within " + 24 + " hours");
            }

            // STEP 4: set order address
            checkoutActivities.setOrderAddress(currentOrderId, shippingAddress);
            log.info("User {} provided address for order {}", bidder.id(), currentOrderId);

            // TODO: may add a payment notification step

            // STEP 5: wait for payment authorization
            boolean isPaymentAuthorized = Workflow.await(
                    Duration.ofMinutes(15), // TODO make dynamic
                    () -> paymentAuthorized || checkoutCancelled
            );

            if (checkoutCancelled) {
                throw new CheckoutCancelledException("Checkout cancelled during payment authorization: " + cancellationReason);
            }

            if (!paymentAuthorized) {
                throw new CheckoutTimeoutException("Payment not authorized within " + 15 + " minutes");
            }

            // STEP 7: associate payment intent with order
            checkoutActivities.setOrderPaymentIntentId(currentOrderId, currentPaymentIntentId);
            log.info("User {} authorized payment for order {}, auction {}", bidder.id(), currentOrderId, request.auction().id());

            checkoutActivities.setOrderPaymentStatus(currentOrderId, PaymentStatus.AUTHORIZED);

            // STEP 8: capture payment
            checkoutActivities.capturePayment(currentOrderId);
            saga.addCompensation(() -> checkoutActivities.refundPayment(currentOrderId));

            checkoutActivities.setOrderPaymentStatus(currentOrderId, PaymentStatus.CAPTURED);

            // STEP 9: emit auction completed event
            auctionActivities.emitAuctionCompletedEvent(request.auction().id(), currentOrderId);

            // STEP 10: mark order as completed
            checkoutActivities.markOrderAsCompleted(currentOrderId);

            log.info("Successful checkout completed for bidder {}", bidder.id());
            return true;
        } catch (CheckoutTimeoutException | CheckoutCancelledException e) {
            log.warn("Checkout failed for bidder {}: {}", bidder.id(), e.getMessage());
            saga.compensate();
            handleCheckoutFailure(e.getMessage());
            return false; // try next bidder
        } catch (Exception e) {
            log.warn("Unexpected checkout error for bidder {}: {}", bidder.id(), e.getMessage());
            saga.compensate();
            handleCheckoutFailure(e.getMessage());
            return false;
        }
    }

    private void handleCheckoutFailure(String reason) {
        log.warn("Checkout failed for order: {}, reason: {}", currentOrderId, reason);
        checkoutActivities.markOrderAsFailed(currentOrderId);

        log.info("Cancelling unauthorized payment intent for failed order: {}", currentOrderId);
        checkoutActivities.cancelPaymentIntent(currentOrderId);
    }

    @Override
    public void submitShippingAddress(UUID orderId, Address address) {
        log.info("Signal received: submitShippingAddress for order: {}", orderId);

        if (!orderId.equals(currentOrderId)) {
            log.warn("Ignoring address submission for non-current order: {} (current: {})", orderId, currentOrderId);
            return;
        }

        if (addressProvided) {
            log.warn("Address already provided for order {}, ignoring duplicate submission", orderId);
            return;
        }

        if (checkoutCancelled) {
            log.warn("Checkout is cancelled for order {}, ignoring address submission", orderId);
            return;
        }

        this.shippingAddress = address;
        this.addressProvided = true;

        log.info("Shipping address accepted for order {}", orderId);
    }

    @Override
    public void paymentAuthorized(UUID orderId, String paymentIntentId) {
        log.info("Signal received: paymentAuthorized for order: {}, intentId: {}", orderId, paymentIntentId);

        if (!orderId.equals(currentOrderId)) {
            log.warn("Ignoring payment authorization for non-current order: {} (current: {})", orderId, currentOrderId);
            return;
        }

        if (!addressProvided) {
            log.warn("Cannot authorize payment before address is provided for order {}", orderId);
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

        log.info("Checkout cancellation accepted for order: {}", orderId);
    }

    private void resetWorkflowState() {
        this.currentOrderId = null;
        this.currentPaymentIntentId = null;
        this.shippingAddress = null;
        this.addressProvided = false;
        this.paymentAuthorized = false;
        this.checkoutCancelled = false;
        this.cancellationReason = null;
    }
}
