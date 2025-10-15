package com.mazadak.orders.dto.request;

import com.mazadak.orders.model.enumeration.OrderStatus;
import com.mazadak.orders.model.enumeration.OrderType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderFilterDto(
        UUID buyerId,
        List<UUID> sellerIds,
        OrderType type,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        OrderStatus status,
        List<UUID> productIds,
        UUID auctionId,
        UUID cartId
) {
}
