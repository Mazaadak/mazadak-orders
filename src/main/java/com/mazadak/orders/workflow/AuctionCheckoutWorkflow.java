package com.mazadak.orders.workflow;

import com.mazadak.orders.dto.internal.AuctionCheckoutRequest;
import com.mazadak.orders.model.entity.Address;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.UUID;

@WorkflowInterface
public interface AuctionCheckoutWorkflow {
    @WorkflowMethod
    void processAuctionCheckout(AuctionCheckoutRequest request);

    @SignalMethod
    void submitShippingAddress(UUID orderId, Address address);

    @SignalMethod
    void paymentAuthorized(UUID orderId, String paymentIntentId);

    @SignalMethod
    void cancelCheckout(UUID orderId, String reason);
}
