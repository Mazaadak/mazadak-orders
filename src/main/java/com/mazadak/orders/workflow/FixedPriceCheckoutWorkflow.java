package com.mazadak.orders.workflow;

import com.mazadak.orders.dto.request.CheckoutRequest;
import com.mazadak.orders.dto.response.OrderResponse;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.UUID;

@WorkflowInterface
public interface FixedPriceCheckoutWorkflow {
    @WorkflowMethod
    OrderResponse processCheckout(CheckoutRequest request);
}
