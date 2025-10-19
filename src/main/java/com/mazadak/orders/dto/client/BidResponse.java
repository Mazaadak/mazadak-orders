package com.mazadak.orders.dto.client;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;


@Schema(
        name = "BidResponse",
        description = "Bid details returned by the API"
)
@Value
public class BidResponse implements Serializable {

    @Schema(description = "Unique identifier of the bid", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    UUID id;

    @Schema(description = "Identifier of the auction this bid belongs to", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    UUID auctionId;

    @Schema(description = "Identifier of the user who placed the bid", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    UUID bidderId;

    @Schema(description = "Bid amount", example = "100.00")
    BigDecimal amount;

    @Schema(description = "Idempotency key used when placing the bid", example = "a1b2c3d4-e5f6-7g8h-9i0j")
    String idempotencyKey;
}