package com.mazadak.orders.dto.event;

import java.util.UUID;

public record AuctionCompletedEvent(UUID auctionId, UUID orderId) {
}
