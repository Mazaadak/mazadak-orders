package com.mazadak.orders.service;

import com.mazadak.orders.dto.request.CheckoutRequest;
import com.mazadak.orders.dto.request.OrderFilterDto;
import com.mazadak.orders.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {
    OrderResponse getOrderById(UUID id);
    Page<OrderResponse> findOrdersByCriteria(OrderFilterDto filter, Pageable pageable);
    OrderResponse checkout(CheckoutRequest request);
    void markCompleted(UUID orderId);
    void markFailed(UUID orderId);
    void createOrderForWinner(UUID auctionId, UUID bidderId);
}
