package com.mazadak.orders.event.consumer;

import com.mazadak.orders.service.OrderService;
import com.mazadak.orders.workflow.starter.AuctionCheckoutStarter;
import com.mazadak.orders.dto.event.PaymentAuthorizedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Consumer;

@Component("paymentAuthorizedEventConsumer")
@Slf4j
@RequiredArgsConstructor
public class PaymentAuthorizedEventConsumer implements Consumer<PaymentAuthorizedEvent> {
    private final OrderService orderService;

    @Override
    public void accept(PaymentAuthorizedEvent event) {
        log.info("Received PaymentAuthorizedEvent for order {}, intent {}",
                event.orderId(), event.paymentIntentId());

        orderService.authorizePayment(UUID.fromString(event.orderId()), event.paymentIntentId());
    }
}
