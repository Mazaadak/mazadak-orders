package com.mazadak.orders.repository.specification;

import com.mazadak.orders.dto.request.OrderFilterDto;
import com.mazadak.orders.model.entity.Order;
import com.mazadak.orders.model.enumeration.OrderStatus;
import com.mazadak.orders.model.enumeration.OrderType;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class OrderSpecifications {
    public static Specification<Order> hasBuyerId(UUID buyerId) {
        return (root, query, builder) -> {
            if (buyerId == null) return null;
            return builder.equal(root.get("buyerId"), buyerId);
        };
    }

    public static Specification<Order> hasSellerIds(List<UUID> sellerIds) {
        return (root, query, builder) -> {
            if (sellerIds == null || sellerIds.isEmpty()) {
                return null;
            }

            Join<Object, Object> items = root.join("orderItems");

            query.distinct(true);

            return items.get("sellerId").in(sellerIds);
        };
    }

    public static Specification<Order> hasType(OrderType type) {
        return (root, query, builder) -> {
            if (type == null) return null;
            return builder.equal(root.get("type"), type);
        };
    }

    public static Specification<Order> hasAmountBetween(BigDecimal min, BigDecimal max) {
        return (root, query, builder) -> {
            if (min == null && max == null) return null;
            if (min == null) return builder.lessThanOrEqualTo(root.get("totalAmount"), max);
            if (max == null) return builder.greaterThanOrEqualTo(root.get("totalAmount"), min);
            return builder.between(root.get("totalAmount"), min, max);
        };
    }

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, builder) -> {
            if (status == null) return null;
            return builder.equal(root.get("status"), status);
        };
    }

    public static Specification<Order> hasProductIds(List<UUID> productIds) {
        return (root, query, builder) -> {
            if (productIds == null || productIds.isEmpty()) {
                return null;
            }

            Join<Object, Object> items = root.join("orderItems");

            query.distinct(true);

            return items.get("productId").in(productIds);
        };
    }

    public static Specification<Order> hasAuctionId(UUID auctionId) {
        return (root, query, builder) -> {
            if (auctionId == null) return null;
            return builder.equal(root.get("auctionId"), auctionId);
        };
    }

    public static Specification<Order> hasCartId(UUID cartId) {
        return (root, query, builder) -> {
            if (cartId == null) return null;
            return builder.equal(root.get("cartId"), cartId);
        };
    }

    public static Specification<Order> buildFromFilter(OrderFilterDto filter) {
        return Specification.allOf(
                hasBuyerId(filter.buyerId()),
                hasSellerIds(filter.sellerIds()),
                hasType(filter.type()),
                hasAmountBetween(filter.minAmount(), filter.maxAmount()),
                hasStatus(filter.status()),
                hasProductIds(filter.productIds()),
                hasAuctionId(filter.auctionId()),
                hasCartId(filter.cartId())
        );
    }
}