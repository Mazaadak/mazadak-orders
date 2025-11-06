package com.mazadak.orders.mapper;

import com.mazadak.orders.dto.response.OrderResponse;
import com.mazadak.orders.model.entity.Order;

public class OrderMapper {

    public static OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getBuyerId(),
                order.getType(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getShippingAddress(),
                order.getPaymentStatus(),
                OrderItemMapper.toResponse(order.getOrderItems()),
                order.getPaymentIntentId(),
                order.getClientSecret(),
                order.getAuctionId(),
                order.getCartId()
        );
    }
}
