package com.mazadak.orders.config;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
public class RetryOptionsProperties {
    private int maximumAttempts;
    private Duration initialInterval;
    private double backoffCoefficient;
}
