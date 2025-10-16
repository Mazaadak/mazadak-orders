package com.mazadak.orders.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem extends BaseEntity{
    private UUID productId;

    private UUID sellerId;

    private String productName;

    private String productImageUrl;

    private BigDecimal unitPrice;

    private int quantity;

    // TODO: make it transient?
    private BigDecimal subtotal;

    @ToString.Exclude
    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}
