package com.mazadak.orders.exception;

public class CheckoutCancelledException extends RuntimeException {
    public CheckoutCancelledException(String message) {
        super(message);
    }
}
