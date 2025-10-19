package com.mazadak.orders.event.consumer;

import com.mazadak.orders.client.AuctionClient;
import com.mazadak.orders.mapper.AuctionCheckoutMapper;
import com.mazadak.orders.dto.internal.AuctionCheckoutRequest;
import com.mazadak.orders.workflow.starter.AuctionCheckoutStarter;
import com.mazadak.orders.dto.event.AuctionEndedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component("auctionEndedEventConsumer")
@RequiredArgsConstructor
@Slf4j
public class AuctionEndedEventConsumer implements Consumer<AuctionEndedEvent> {
    private final AuctionCheckoutStarter starter;
    private final AuctionClient client;

    @Override
    public void accept(AuctionEndedEvent event) {
        log.info("Received AuctionEndedEvent: {}", event);

        if (event.auction() == null) {
            log.error("Auction is null in event: {}", event);
            return;
        }

        log.info("Received AuctionEndedEvent for auction {}", event.auction().id());
        AuctionCheckoutRequest request = AuctionCheckoutMapper.fromEndedEventToCheckoutRequest(event);
        starter.startAuctionCheckout(request);
    }
}
