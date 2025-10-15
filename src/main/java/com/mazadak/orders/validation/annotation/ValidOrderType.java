package com.mazadak.orders.validation.annotation;

import com.mazadak.orders.validation.validator.OrderTypeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OrderTypeValidator.class)
@Documented
public @interface ValidOrderType {
    String message() default "An order must be either associated with an auction or a cart";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
