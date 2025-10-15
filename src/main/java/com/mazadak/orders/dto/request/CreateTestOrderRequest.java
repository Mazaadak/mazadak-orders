package com.mazadak.orders.dto.request;

import com.mazadak.orders.model.enumeration.OrderType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
public class CreateTestOrderRequest {
    @NotNull
    private UUID buyerId;

    @NotNull
    private OrderType type;

    private UUID cartId;

    private UUID auctionId;

    @NotNull
    private TestAddressRequest shippingAddress;

    @NotEmpty
    private List<TestOrderItemRequest> items;
}
