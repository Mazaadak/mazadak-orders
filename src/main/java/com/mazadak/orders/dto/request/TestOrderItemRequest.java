package com.mazadak.orders.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TestOrderItemRequest {
    @NotNull
    private UUID productId;

    @NotBlank
    private String productName;

    private String productImageUrl;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal unitPrice;

    @Min(1)
    private int quantity;
}
