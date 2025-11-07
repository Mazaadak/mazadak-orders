package com.mazadak.orders.service;

import com.mazadak.orders.dto.client.AuctionResponse;
import com.mazadak.orders.dto.client.CartResponseDTO;
import com.mazadak.orders.dto.request.CheckoutRequest;
import com.mazadak.orders.dto.request.OrderFilterDto;
import com.mazadak.orders.dto.response.OrderResponse;
import com.mazadak.orders.model.entity.Address;
import com.mazadak.orders.model.enumeration.PaymentStatus;
import com.mazadak.orders.dto.internal.AuctionCheckoutRequest;
import com.mazadak.orders.model.entity.Order;
import io.temporal.api.common.v1.WorkflowExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {
    OrderResponse getOrderById(UUID id);
    Page<OrderResponse> findOrdersByCriteria(OrderFilterDto filter, Pageable pageable);
    WorkflowExecution checkout(UUID idempotencyKey, CheckoutRequest request);
    void markCompleted(UUID orderId);
    void markFailed(UUID orderId);
    void markCancelled(UUID orderId);
    UUID createOrderForWinner(AuctionResponse auction, AuctionCheckoutRequest.BidderInfo bidder);
    void setAddress(UUID orderId, Address address);
    void setPaymentIntentId(UUID orderId, String paymentIntentId);
    void setPaymentStatus(UUID orderId, PaymentStatus status);
    OrderResponse createFixedPriceOrder(CheckoutRequest request, CartResponseDTO cart, UUID idempotencyKey);

    void assertOrderBelongsToBuyer(UUID orderId, UUID userId);

    void cancelCheckout(UUID orderId);
    String getWorkflowIdForOrder(Order order);
    String getWorkflowIdByOrderId(UUID orderId);
    void authorizePayment(UUID orderId, String paymentIntentId);

    void attachIntent(UUID orderId, String paymentIntentId, String clientSecret);

    void setClientSecret(UUID currentOrderId, String clientSecret);

    void assertAmountNotTooLarge(UUID orderId);
}
