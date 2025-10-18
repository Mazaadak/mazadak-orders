package com.mazadak.orders.dto.request;


import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ConfirmReservationRequest(
        List<UUID> reservationIds,
        @NotNull  UUID orderId ){
}
