package com.mazadak.orders.config;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

//@Service
//@RequiredArgsConstructor
public class ActivitiesConfigFactory {
//    private final CheckoutActivitiesProperties properties;
//
//    public ActivityOptions buildAuctionOptions() {
//        var auction = properties.getAuction();
//        var retry = auction.getRetryOptions();
//
//        return ActivityOptions.newBuilder()
//                .setStartToCloseTimeout(auction.getStartToCloseTimeout())
//                .setRetryOptions(RetryOptions.newBuilder()
//                        .setMaximumAttempts(retry.getMaximumAttempts())
//                        .setInitialInterval(retry.getInitialInterval())
//                        .setBackoffCoefficient(retry.getBackoffCoefficient())
//                        .build()
//                ).build();
//    }
//
//    public ActivityOptions buildSharedOptions() {
//        var shared = properties.getShared();
//        var retry = shared.getRetryOptions();
//
//        return ActivityOptions.newBuilder()
//                .setStartToCloseTimeout(shared.getStartToCloseTimeout())
//                .setRetryOptions(RetryOptions.newBuilder()
//                        .setMaximumAttempts(retry.getMaximumAttempts())
//                        .setInitialInterval(retry.getInitialInterval())
//                        .setBackoffCoefficient(retry.getBackoffCoefficient())
//                        .build()
//                ).build();
//    }
}
