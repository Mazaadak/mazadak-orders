package com.mazadak.orders.service;

import com.mazadak.orders.dto.client.AuctionResponse;
import com.mazadak.orders.dto.request.CheckoutRequest;
import com.mazadak.orders.dto.request.OrderFilterDto;
import com.mazadak.orders.dto.response.OrderResponse;
import com.mazadak.orders.model.entity.Address;
import com.mazadak.orders.model.enumeration.PaymentStatus;
import com.mazadak.orders.dto.internal.AuctionCheckoutRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {
    OrderResponse getOrderById(UUID id);
    Page<OrderResponse> findOrdersByCriteria(OrderFilterDto filter, Pageable pageable);
    OrderResponse checkout(CheckoutRequest request);
    void markCompleted(UUID orderId);
    void markFailed(UUID orderId);
    void markCancelled(UUID orderId);
    UUID createOrderForWinner(AuctionResponse auction, AuctionCheckoutRequest.BidderInfo bidder);
    void setAddress(UUID orderId, Address address);
    void setPaymentIntentId(UUID orderId, String paymentIntentId);
    void setPaymentStatus(UUID orderId, PaymentStatus status);
}
