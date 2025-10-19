package com.mazadak.orders.dto.event;

import com.mazadak.orders.dto.client.AuctionResponse;
import com.mazadak.orders.dto.internal.AuctionCheckoutRequest;

import java.util.List;

public record AuctionEndedEvent(AuctionResponse auction, List<AuctionCheckoutRequest.BidderInfo> bidders) {
}