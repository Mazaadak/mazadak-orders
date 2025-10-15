package com.mazadak.orders.mapper;

import com.mazadak.orders.dto.response.OrderItemResponse;
import com.mazadak.orders.model.entity.OrderItem;

import java.util.List;

public class OrderItemMapper {
    public static List<OrderItemResponse> toResponse(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getSellerId(),
                        item.getProductName(),
                        item.getProductImageUrl(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getSubtotal()
                )).toList();
    }
}
