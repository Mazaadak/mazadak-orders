package com.mazadak.orders.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

//@Component
//@ConfigurationProperties(prefix = "app.checkout.activities")
//@Getter
public class CheckoutActivitiesProperties {
    private ActivityOptionsProperties auction;
    private ActivityOptionsProperties shared;
}
