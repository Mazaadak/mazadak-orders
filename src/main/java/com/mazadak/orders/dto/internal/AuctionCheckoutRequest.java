package com.mazadak.orders.dto.internal;

import com.mazadak.orders.dto.client.AuctionResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AuctionCheckoutRequest(
        AuctionResponse auction,
        List<BidderInfo> bidders
) {
    public record BidderInfo(
            UUID id,
            BigDecimal amount
    ) {
    }
}
