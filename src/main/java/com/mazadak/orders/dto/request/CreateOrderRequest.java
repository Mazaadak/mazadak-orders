package com.mazadak.orders.dto.request;

import com.mazadak.orders.model.entity.Address;
import com.mazadak.orders.model.enumeration.OrderType;
import com.mazadak.orders.validation.annotation.ValidOrderType;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link com.mazadak.orders.model.entity.Order}
 */
@ValidOrderType
public record CreateOrderRequest(@NotNull(message = "An order must be associated with a buyer") UUID buyerId,
                                 @NotNull(message = "An order must be associated with a seller") UUID sellerId,
                                 @NotNull(message = "An order must have a type (AUCTION or FIXED_PRICE)") OrderType type,
                                 Address shippingAddress, UUID auctionId, UUID cartId) implements Serializable {
}