package com.mazadak.orders.dto.request;

import java.util.List;
import java.util.UUID;

public record ReserveInventoryRequest(
        List<InventoryReservationItem> items,
        UUID orderId
) { }
