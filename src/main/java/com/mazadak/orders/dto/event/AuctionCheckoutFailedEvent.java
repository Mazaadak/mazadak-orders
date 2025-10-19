package com.mazadak.orders.dto.event;

import com.mazadak.orders.dto.internal.AuctionCheckoutRequest;

import java.util.UUID;

public record AuctionCheckoutFailedEvent(UUID orderId, String email, AuctionCheckoutRequest.BidderInfo bidder) {
}
