package com.mazadak.orders.dto.response;

import com.mazadak.orders.model.entity.Address;
import com.mazadak.orders.model.enumeration.OrderStatus;
import com.mazadak.orders.model.enumeration.OrderType;
import com.mazadak.orders.model.enumeration.PaymentStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link com.mazadak.orders.model.entity.Order}
 */
public record OrderResponse(
        UUID id,
        UUID buyerId,
        OrderType type,
        BigDecimal totalAmount,
        OrderStatus status,
        Address shippingAddress,
        PaymentStatus paymentStatus,
        List<OrderItemResponse> orderItems,
        String paymentIntentId,
        String clientSecret,
        UUID auctionId,
        UUID cartId,
        Instant createdAt
) implements Serializable { }