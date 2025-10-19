package com.mazadak.orders.workflow.activity.impl;

import com.mazadak.orders.client.CartClient;
import com.mazadak.orders.client.InventoryClient;
import com.mazadak.orders.dto.client.CartItemResponseDTO;
import com.mazadak.orders.dto.client.CartResponseDTO;
import com.mazadak.orders.dto.request.*;
import com.mazadak.orders.dto.response.OrderResponse;
import com.mazadak.orders.repository.OrderRepository;
import com.mazadak.orders.service.OrderService;
import com.mazadak.orders.workflow.activity.FixedPriceCheckoutActivities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class FixedPriceCheckoutActivitiesImpl implements FixedPriceCheckoutActivities {
    private final CartClient cartClient;
    private final InventoryClient inventoryClient;
    private final OrderService orderService;

    @Override
    public OrderResponse createOrder(CheckoutRequest request, CartResponseDTO cart) {
        log.info("Creating order for user {} and cart {}", request.userId(), cart.cartId());
        return orderService.createFixedPriceOrder(request, cart);
    }

    @Override
    public void deactivateCart(UUID userId) {
        log.info("Deactivating cart for user {}", userId);
        cartClient.deactivateCart(userId);
    }

    @Override
    public CartResponseDTO getCart(UUID userId) {
        log.info("Getting cart for user {}", userId);
       return cartClient.getCart(userId).getBody();
    }

    @Override
    public void activateCart(UUID userId) {
        log.info("Activating cart for user {}", userId);
        cartClient.activateCart(userId);
    }

    @Override
    public void clearCart(UUID userId) {
        log.info("Clearing cart for user {}", userId);
        cartClient.clearCart(userId);
    }

    @Override
    public List<UUID> reserveInventory(UUID orderId, List<CartItemResponseDTO> items) {
        log.info("Reserving inventory for order {} and items {}", orderId, items);
        ReserveInventoryRequest request = new ReserveInventoryRequest(
                items.stream()
                        .map(item -> new InventoryReservationItem(
                                item.productId(),
                                item.quantity()
                        ))
                        .toList(),
                orderId
        );
        UUID idempotencyKey = UUID.randomUUID();
        return inventoryClient.reserveInventory(idempotencyKey, request).getBody();
    }

    @Override
    public void confirmInventoryReservations(UUID orderId, List<UUID> reservationIds) {
        log.info("Confirming inventory reservations for order {} and reservationIds {}", orderId, reservationIds);
        ConfirmReservationRequest request = new ConfirmReservationRequest(reservationIds, orderId);
        UUID idempotencyKey = UUID.randomUUID();
        inventoryClient.confirmInventoryReservations(idempotencyKey, request);
        log.info("Confirmed inventory reservations for order {} and reservationIds {}", orderId, reservationIds);
    }

    @Override
    public void releaseInventoryReservations(UUID orderId, List<UUID> reservationIds) {
        log.info("Releasing inventory reservations for order {} and reservationIds {}", orderId, reservationIds);
        ReleaseReservationRequest request =new ReleaseReservationRequest(reservationIds, orderId);
        UUID idempotencyKey = UUID.randomUUID();
        inventoryClient.releaseInventoryReservations(idempotencyKey, request);
        log.info("Released inventory reservations for order {} and reservationIds {}", orderId, reservationIds);
    }

}
