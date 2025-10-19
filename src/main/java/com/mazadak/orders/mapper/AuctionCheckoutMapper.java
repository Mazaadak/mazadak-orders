package com.mazadak.orders.mapper;

import com.mazadak.orders.dto.event.AuctionEndedEvent;
import com.mazadak.orders.dto.internal.AuctionCheckoutRequest;

public class AuctionCheckoutMapper {
    public static AuctionCheckoutRequest fromEndedEventToCheckoutRequest(AuctionEndedEvent event) {
        return new AuctionCheckoutRequest(event.auction(), event.bidders());
    }
}
