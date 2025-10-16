package com.mazadak.orders.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@Getter
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CartOwnershipException extends RuntimeException {
    private final UUID cartId, userId;

    public CartOwnershipException(String message, UUID cartId, UUID userId) {
        this.cartId = cartId;
        this.userId = userId;
    }
}
