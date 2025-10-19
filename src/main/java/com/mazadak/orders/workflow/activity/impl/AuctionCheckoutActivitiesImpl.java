package com.mazadak.orders.workflow.activity.impl;

import com.mazadak.orders.dto.client.AuctionResponse;
import com.mazadak.orders.dto.event.AuctionCompletedEvent;
import com.mazadak.orders.dto.event.AuctionInvalidEvent;
import com.mazadak.orders.service.OrderService;
import com.mazadak.orders.dto.event.AuctionCheckoutFailedEvent;
import com.mazadak.orders.dto.internal.AuctionCheckoutRequest;
import com.mazadak.orders.dto.event.AuctionCheckoutStartedEvent;
import com.mazadak.orders.workflow.activity.AuctionCheckoutActivities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class AuctionCheckoutActivitiesImpl implements AuctionCheckoutActivities {
    private final StreamBridge streamBridge;
    private final OrderService orderService;

    @Override
    public UUID createOrderForWinner(AuctionResponse auction, AuctionCheckoutRequest.BidderInfo bidder) {
        return orderService.createOrderForWinner(auction, bidder);
    }

    @Override
    public void notifyWinnerToCheckout(AuctionCheckoutRequest.BidderInfo bidder, String email, UUID orderId) {
        log.info("User {} should checkout for order {}", bidder.id(), orderId);
        var checkoutUrl = "http://localhost:3000/checkout/" + orderId; // TODO: configure
        streamBridge.send("auctionCheckoutStarted-out-0", new AuctionCheckoutStartedEvent(orderId, email, bidder, checkoutUrl)); // TODO extract binding name to constant wrapper class
    }

    @Override
    public void emitAuctionCompletedEvent(UUID auctionId, UUID orderId) {
        log.info("Sending auction completed event for auction {}", auctionId);
        streamBridge.send("auctionCompleted-out-0", new AuctionCompletedEvent(auctionId, orderId)); // TODO extract binding name to constant wrapper class
    }

    @Override
    public void emitAuctionInvalidEvent(UUID auctionId) {
        log.info("Sending auction invalid event for auction {}", auctionId);
        streamBridge.send("auctionInvalid-out-0", new AuctionInvalidEvent(auctionId)); // TODO extract binding name to constant wrapper class
    }

    @Override
    public void notifyWinnerCheckoutFailed(AuctionCheckoutRequest.BidderInfo bidder, String email, UUID orderId) {
        log.info("User {} failed to checkout for order {}", bidder.id(), orderId);
        streamBridge.send("auctionCheckoutFailed-out-0", new AuctionCheckoutFailedEvent(orderId, email, bidder)); // TODO extract binding name to constant wrapper class
    }
}
