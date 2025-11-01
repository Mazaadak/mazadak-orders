package com.mazadak.orders.dto.internal;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class WorkflowResult {
    private boolean success;
    private String status; // "ACTIVE", "FAILED", "ROLLED_BACK"
    private String errorMessage;
}