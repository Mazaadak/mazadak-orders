package com.mazadak.orders.exception;

public class CheckoutTimeoutException extends RuntimeException {
    public CheckoutTimeoutException(String message) {
        super(message);
    }
}
