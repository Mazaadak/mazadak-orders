package com.mazadak.orders.validation.validator;

import com.mazadak.orders.model.enumeration.OrderType;
import com.mazadak.orders.validation.annotation.ValidOrderType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.UUID;

public class OrderTypeValidator implements ConstraintValidator<ValidOrderType, Object> {
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            var type = (OrderType) value.getClass().getMethod("type").invoke(value);
            var cartId = (UUID) value.getClass().getMethod("cartId").invoke(value);
            var auctionId = (UUID) value.getClass().getMethod("auctionId").invoke(value);

            if (cartId != null && auctionId != null) {
                return false;
            } // an order has to be associated with either, not both

            if (cartId != null && type == OrderType.FIXED_PRICE) {
                return true;
            } // a FIXED_PRICE order has to be associated with a cart

            if (auctionId != null && type == OrderType.AUCTION) {
                return true;
            } // an AUCTION order has to be associated with an auction

            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
