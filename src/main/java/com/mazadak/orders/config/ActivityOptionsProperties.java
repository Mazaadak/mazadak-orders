package com.mazadak.orders.config;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
public class ActivityOptionsProperties {
    private Duration startToCloseTimeout;
    private RetryOptionsProperties retryOptions;
}
