package com.mazadak.orders.workflow.starter;

import com.mazadak.orders.dto.request.CheckoutRequest;
import com.mazadak.orders.service.OrderService;
import com.mazadak.orders.workflow.FixedPriceCheckoutWorkflow;
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
    private WorkflowClient client;
    private final OrderService orderService;

    public void startFixedPriceCheckout(UUID idempotencyKey, CheckoutRequest request) {
        String workflowId = "fixed-price-checkout-" + idempotencyKey;

        FixedPriceCheckoutWorkflow workflow = client.newWorkflowStub(
                FixedPriceCheckoutWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue("FIXED_PRICE_CHECKOUT_TASK_QUEUE")
                        .setWorkflowId(workflowId)
                        .build()
        );

        var exec = WorkflowClient.start(workflow::processCheckout, request);
        log.info("Workflow started: workflowId={}, runId={}", exec.getWorkflowId(), exec.getRunId());

    }
}
