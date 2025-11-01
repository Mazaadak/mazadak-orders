package com.mazadak.orders.dto.internal;

public record CheckoutStatusResponse(
        String status,        // "RUNNING", "COMPLETED", "FAILED"
        String listingStatus, // "ACTIVE", "FAILED", "ROLLED_BACK" (only when completed)
        String errorMessage
) {}
