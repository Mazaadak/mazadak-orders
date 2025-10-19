package com.mazadak.orders.dto.event;

import java.util.UUID;

public record AuctionInvalidEvent(UUID auctionId) {
}
