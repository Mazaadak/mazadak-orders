package com.mazadak.orders.workflow;

import com.mazadak.orders.dto.internal.WorkflowResult;
import com.mazadak.orders.dto.request.CheckoutRequest;
import com.mazadak.orders.dto.response.OrderResponse;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.UUID;

@WorkflowInterface
public interface FixedPriceCheckoutWorkflow {
    @WorkflowMethod
    WorkflowResult processCheckout(CheckoutRequest request, UUID idempotencyKey);

    @SignalMethod
    void paymentAuthorized(UUID orderId, String paymentIntentId);

    @SignalMethod
    void cancelCheckout(UUID orderId, String reason);
}
