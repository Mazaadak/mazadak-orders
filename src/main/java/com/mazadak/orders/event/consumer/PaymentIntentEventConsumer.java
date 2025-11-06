package com.mazadak.orders.event.consumer;

import com.mazadak.orders.dto.event.PaymentIntentCreatedEvent;
import com.mazadak.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Consumer;

@Component("intentCreatedEventConsumer")
@Slf4j
@RequiredArgsConstructor
public class PaymentIntentEventConsumer implements Consumer<PaymentIntentCreatedEvent> {
    private final OrderService orderService;

    @Override
    public void accept(PaymentIntentCreatedEvent event) {
        log.info("Received PaymentIntentCreatedEvent for order {}, intent {}",
                event.orderId(), event.paymentIntentId());

        orderService.attachIntent(UUID.fromString(event.orderId()), event.paymentIntentId(), event.clientSecret());
    }
}
