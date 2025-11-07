package com.mazadak.orders.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.math.BigDecimal;
import java.util.UUID;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AmountTooLargeException extends RuntimeException {
    private static final double TRANSACTION_LIMIT = 999999;
    public AmountTooLargeException(UUID orderId, BigDecimal totalAmount) {
        super(String.format("Order %s total amount is above limit (total amount is %f, limit is %f", orderId, totalAmount.doubleValue(), TRANSACTION_LIMIT));
    }
}
