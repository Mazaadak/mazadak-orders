package com.mazadak.orders.service;

import com.mazadak.orders.dto.request.OrderFilterDto;
import com.mazadak.orders.dto.response.OrderResponse;
import com.mazadak.orders.model.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {
    OrderResponse getOrderById(UUID id);
    Page<OrderResponse> findOrdersByCriteria(OrderFilterDto filter, Pageable pageable);
    OrderResponse createOrder(Order order);
}
