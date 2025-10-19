package com.mazadak.orders.client;

import com.mazadak.orders.dto.client.PaymentIntentResponse;
import com.mazadak.orders.dto.client.RefundRequest;
import com.mazadak.orders.dto.client.RefundResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "payment")
public interface PaymentClient {
    @PostMapping("/api/payments/{orderId}/capture")
    public ResponseEntity<PaymentIntentResponse> capturePayment(@PathVariable UUID orderId);

    @PostMapping("/api/payments/{orderId}/cancel")
    public ResponseEntity<PaymentIntentResponse> cancelPayment(@PathVariable UUID orderId);

    @PostMapping("/api/payments/refund")
    public ResponseEntity<RefundResponse> refundPayment(@RequestBody RefundRequest request);
}
