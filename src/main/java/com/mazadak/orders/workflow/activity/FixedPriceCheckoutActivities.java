package com.mazadak.orders.workflow.activity;

import com.mazadak.orders.dto.client.CartItemResponseDTO;
import com.mazadak.orders.dto.client.CartResponseDTO;
import com.mazadak.orders.dto.request.CheckoutRequest;
import com.mazadak.orders.dto.response.OrderResponse;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.List;
import java.util.UUID;


@ActivityInterface
public interface FixedPriceCheckoutActivities {

    // ==== Order ====

    @ActivityMethod
    OrderResponse createOrder(CheckoutRequest request, CartResponseDTO cart);

    // ==== Cart Activities ====
    @ActivityMethod
    void deactivateCart(UUID userId);

    @ActivityMethod
    CartResponseDTO getCart(UUID userId);

    @ActivityMethod
    void clearCart(UUID userId);

    @ActivityMethod
    void activateCart(UUID userId);

    // ==== Inventory Activities ====
    @ActivityMethod
    List<UUID> reserveInventory(UUID orderId, List<CartItemResponseDTO> items);

    @ActivityMethod
    void confirmInventoryReservations(UUID orderId, List<UUID> reservationIds);

    @ActivityMethod
    void releaseInventoryReservations(UUID orderId, List<UUID> reservationIds);



    // === Payment === TODO:


    // === Notifications === TODO:
}
