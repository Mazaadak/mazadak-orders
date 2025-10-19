package com.mazadak.orders.dto.client;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AuctionResponse(
        UUID id,
        UUID productId,
        UUID sellerId,
        String title,
        BigDecimal startingPrice,
        BigDecimal reservePrice,
        BidResponse highestBidPlaced,
        BigDecimal bidIncrement,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AuctionStatus status
) implements Serializable {

}