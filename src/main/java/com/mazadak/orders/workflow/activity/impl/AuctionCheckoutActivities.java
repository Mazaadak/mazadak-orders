package com.mazadak.orders.workflow.activity.impl;

import com.mazadak.orders.dto.client.AuctionResponse;
import com.mazadak.orders.dto.internal.AuctionCheckoutRequest;
import io.temporal.activity.ActivityInterface;

import java.util.UUID;

@ActivityInterface
public interface AuctionCheckoutActivities {
    UUID createOrderForWinner(AuctionResponse auction, AuctionCheckoutRequest.BidderInfo bidder);
    void notifyWinnerToCheckout(AuctionCheckoutRequest.BidderInfo bidder, String email, UUID orderId);
    void notifyWinnerCheckoutFailed(AuctionCheckoutRequest.BidderInfo bidder, String email, UUID orderId);
    void emitAuctionCompletedEvent(UUID auctionId, UUID orderId);
    void emitAuctionInvalidEvent(UUID auctionId);
}
