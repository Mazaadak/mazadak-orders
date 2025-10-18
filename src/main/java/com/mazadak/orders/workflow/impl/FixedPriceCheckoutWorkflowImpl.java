package com.mazadak.orders.workflow.impl;

import com.mazadak.orders.workflow.activities.FixedPriceCheckoutActivities;
import com.mazadak.orders.dto.client.CartResponseDTO;
import com.mazadak.orders.dto.request.CheckoutRequest;
import com.mazadak.orders.dto.response.OrderResponse;
import com.mazadak.orders.workflow.FixedPriceCheckoutWorkflow;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
public class FixedPriceCheckoutWorkflowImpl implements FixedPriceCheckoutWorkflow {

    private  FixedPriceCheckoutActivities activities ;

    @Override
    public OrderResponse processCheckout(CheckoutRequest request) {

        if (activities == null) {
            this.activities = Workflow.newActivityStub(
                    FixedPriceCheckoutActivities.class,
                    ActivityOptions.newBuilder()
                            .setStartToCloseTimeout(Duration.ofMinutes(10))
                            .build()
            );
        }

        Saga saga = new Saga(new Saga.Options.Builder()
                .setParallelCompensation(false)
                .setContinueWithError(false)
                .build());

        OrderResponse order = null;
        CartResponseDTO cart = null;

        try {
            // 1. Deactivate cart to prevent modifications
            activities.deactivateCart(request.userId());
            saga.addCompensation(() -> activities.activateCart(request.userId()));

            // 2. Get cart
            cart = activities.getCart(request.userId());

            // 3. Create Order
            order = activities.createOrder(request, cart);
            UUID orderId = order.id();
            // If we fail after this, we need to mark the order as failed
            saga.addCompensation(() -> activities.markOrderAsFailed(orderId));

            // 4. Reserve inventory
            List<UUID> reservationIds = activities.reserveInventory(order.id(), cart.cartItems());


            // If we fail after this, we need to release the inventory reservations
            saga.addCompensation(
                    () -> activities.releaseInventoryReservations(orderId, reservationIds)
            );

            // 5. Process payment (TODO: Implement payment processing)


            // 6. If payment successful, confirm reservation
            activities.confirmInventoryReservations(order.id(), reservationIds);


            // 7. Clear and activate cart
            activities.clearCart(request.userId());
            activities.activateCart(request.userId());


            // 8. Send notifications (TODO: Implement notifications)

            // 9. Update order status to be completed
            activities.markOrderAsCompleted(orderId);

            return order;

        } catch (Exception e) {

            Workflow.getLogger(getClass()).error("Error during checkout process", e);

            // Compensate for completed operations in reverse order
            Workflow.newDetachedCancellationScope(saga::compensate).run();

            // If we have an order ID but couldn't mark it as failed in the compensation
            if (order != null && order.id() != null) {
                try {
                    activities.markOrderAsFailed(order.id());
                } catch (Exception ex) {
                    Workflow.getLogger(getClass()).error("Failed to mark order as failed", ex);
                }
            }

            throw Workflow.wrap(e);
        }
    }
}
