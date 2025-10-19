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
    private final OrderService orderService;

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

    public void sendPaymentAuthorized(UUID orderId, String paymentIntentId) {
        String workflowId = "auction-checkout-" + getAuctionIdForOrder(orderId);
        AuctionCheckoutWorkflow workflow = client.newWorkflowStub(
                AuctionCheckoutWorkflow.class,
                workflowId
        );

        workflow.paymentAuthorized(orderId, paymentIntentId);
    }

    public void sendAddressProvided(UUID orderId, Address address) {
        String workflowId = "auction-checkout-" + getAuctionIdForOrder(orderId);
        AuctionCheckoutWorkflow workflow = client.newWorkflowStub(
                AuctionCheckoutWorkflow.class,
                workflowId
        );

        workflow.submitShippingAddress(orderId, address);
    }

    private UUID getAuctionIdForOrder(UUID orderId) {
        return orderService.getOrderById(orderId).auctionId();
    }

}