package com.mazadak.orders.workflow.starter;

import com.mazadak.orders.dto.internal.AuctionCheckoutRequest;
import com.mazadak.orders.model.entity.Address;
import com.mazadak.orders.service.OrderService;
import com.mazadak.orders.workflow.AuctionCheckoutWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuctionCheckoutStarter {
    private final WorkflowClient client;

    public void startAuctionCheckout(AuctionCheckoutRequest request) {
        String workflowId = "auction-checkout-" + request.auction().id();

        AuctionCheckoutWorkflow workflow = client.newWorkflowStub(
                AuctionCheckoutWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue("AUCTION_CHECKOUT_TASK_QUEUE") // TODO: extract into a constant wrapper class
                        .setWorkflowId(workflowId)
                        .build()
        );

        WorkflowClient.start(workflow::processAuctionCheckout, request);
    }

    public void sendPaymentAuthorized(UUID auctionId, String paymentIntentId) {
        String workflowId = "auction-checkout-" + auctionId;
        AuctionCheckoutWorkflow workflow = client.newWorkflowStub(
                AuctionCheckoutWorkflow.class,
                workflowId
        );

        workflow.paymentAuthorized(auctionId, paymentIntentId);
    }

    public void sendAddressProvided(UUID orderId, UUID auctionId, Address address) {
        String workflowId = "auction-checkout-" + auctionId;
        AuctionCheckoutWorkflow workflow = client.newWorkflowStub(
                AuctionCheckoutWorkflow.class,
                workflowId
        );

        workflow.submitShippingAddress(orderId, address);
    }

    public void sendCheckoutCancelled(UUID orderId, UUID auctionId, String reason) {
        String workflowId = "auction-checkout-" + auctionId;
        AuctionCheckoutWorkflow workflow = client.newWorkflowStub(
                AuctionCheckoutWorkflow.class,
                workflowId
        );

        workflow.cancelCheckout(orderId, reason);
    }

}