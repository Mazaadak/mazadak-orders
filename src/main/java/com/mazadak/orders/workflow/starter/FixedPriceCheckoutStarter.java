package com.mazadak.orders.workflow.starter;

import com.mazadak.orders.dto.request.CheckoutRequest;
import com.mazadak.orders.service.OrderService;
import com.mazadak.orders.workflow.AuctionCheckoutWorkflow;
import com.mazadak.orders.workflow.FixedPriceCheckoutWorkflow;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.workflow.Workflow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FixedPriceCheckoutStarter {
    private final WorkflowClient client;

    public WorkflowExecution startFixedPriceCheckout(UUID idempotencyKey, CheckoutRequest request) {
        String workflowId = "fixed-price-checkout-" + idempotencyKey;
        FixedPriceCheckoutWorkflow workflow = client.newWorkflowStub(
                FixedPriceCheckoutWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue("FIXED_PRICE_CHECKOUT_TASK_QUEUE")
                        .setWorkflowId(workflowId)
                        .build()
        );

        var exec = WorkflowClient.start(workflow::processCheckout, request, idempotencyKey);
        log.info("Workflow started: workflowId={}, runId={}", exec.getWorkflowId(), exec.getRunId());
        return exec;
    }

    public void sendPaymentAuthorized(UUID idempotencyKey, UUID orderId, String paymentIntentId) {
        String workflowId = "fixed-price-checkout-" + idempotencyKey;
        FixedPriceCheckoutWorkflow workflow = client.newWorkflowStub(
                FixedPriceCheckoutWorkflow.class,
                workflowId
        );

        workflow.paymentAuthorized(orderId, paymentIntentId);
    }

    public void sendCheckoutCancelled(UUID idempotencyKey, UUID orderId, String reason) {
        String workflowId = "fixed-price-checkout-" + idempotencyKey;
        FixedPriceCheckoutWorkflow workflow = client.newWorkflowStub(
                FixedPriceCheckoutWorkflow.class,
                workflowId
        );

        workflow.cancelCheckout(orderId, reason);
    }

    public void sendIntentCreated(UUID idempotencyKey, UUID orderId, String paymentIntentId, String clientSecret) {
        String workflowId = "fixed-price-checkout-" + idempotencyKey;
        FixedPriceCheckoutWorkflow workflow = client.newWorkflowStub(
                FixedPriceCheckoutWorkflow.class,
                workflowId
        );

        workflow.intentCreated(orderId, paymentIntentId, clientSecret);
    }
}
