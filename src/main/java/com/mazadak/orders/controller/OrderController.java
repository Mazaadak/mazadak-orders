package com.mazadak.orders.controller;

import com.mazadak.orders.dto.internal.CheckoutStatusResponse;
import com.mazadak.orders.dto.internal.WorkflowResult;
import com.mazadak.orders.dto.request.CheckoutRequest;
import com.mazadak.orders.dto.request.CreateTestOrderRequest;
import com.mazadak.orders.dto.request.OrderFilterDto;
import com.mazadak.orders.dto.request.TestOrderItemRequest;
import com.mazadak.orders.dto.response.OrderResponse;
import com.mazadak.orders.model.entity.Address;
import com.mazadak.orders.model.entity.Order;
import com.mazadak.orders.model.entity.OrderItem;
import com.mazadak.orders.model.enumeration.OrderStatus;
import com.mazadak.orders.model.enumeration.OrderType;
import com.mazadak.orders.model.enumeration.PaymentStatus;
import com.mazadak.orders.repository.OrderItemRepository;
import com.mazadak.orders.repository.OrderRepository;
import com.mazadak.orders.service.OrderService;
import com.mazadak.orders.workflow.starter.AuctionCheckoutStarter;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Validated
@Slf4j
public class OrderController {
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AuctionCheckoutStarter auctionCheckoutStarter;
    private final WorkflowClient workflowClient;

    @GetMapping("{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping
    public Page<OrderResponse> findOrdersByCriteria(
            @ModelAttribute OrderFilterDto filter,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return orderService.findOrdersByCriteria(filter, pageable);
    }

//    @PostMapping("/test/create")
//    public ResponseEntity<Order> createTestOrder(@RequestBody CreateTestOrderRequest request) {
//        // TODO: remove
//        Order order = new Order();
//        order.setBuyerId(request.getBuyerId());
//        order.setType(request.getType());
//        order.setStatus(OrderStatus.PENDING);
//        order.setPaymentStatus(PaymentStatus.PENDING);
//        order.setTotalAmount(BigDecimal.ZERO);
//
//        if (request.getType() == OrderType.FIXED_PRICE) {
//            order.setCartId(request.getCartId());
//        } else {
//            order.setAuctionId(request.getAuctionId());
//        }
//
//        // Create shipping address
//        Address address = new Address();
//        address.setStreet(request.getShippingAddress().getStreet());
//        address.setCity(request.getShippingAddress().getCity());
//        address.setState(request.getShippingAddress().getState());
//        address.setPostalCode(request.getShippingAddress().getZipCode());
//        address.setCountry(request.getShippingAddress().getCountry());
//        order.setShippingAddress(address);
//
//        // Save order first to get ID
//        order = orderRepository.save(order);
//
//        // Create order items
//        BigDecimal total = BigDecimal.ZERO;
//        for (TestOrderItemRequest itemRequest : request.getItems()) {
//            OrderItem item = new OrderItem();
//            item.setProductId(itemRequest.getProductId());
//            item.setProductName(itemRequest.getProductName());
//            item.setProductImageUrl(itemRequest.getProductImageUrl());
//            item.setUnitPrice(itemRequest.getUnitPrice());
//            item.setQuantity(itemRequest.getQuantity());
//            item.setOrder(order);
//
//            orderItemRepository.save(item);
//
//            BigDecimal subtotal = itemRequest.getUnitPrice()
//                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
//            total = total.add(subtotal);
//        }
//
//        // Update total amount
//        order.setTotalAmount(total);
//        order = orderRepository.save(order);
//
//        return ResponseEntity.ok(order);
//    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String,String>> checkout(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CheckoutRequest request) {
        if (!userId.equals(request.userId())) {
            log.warn("User {} is not authorized to checkout for order {}", userId, request);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        WorkflowExecution exec = orderService.checkout(idempotencyKey, request);
        return ResponseEntity.accepted().body(Map.of(
                "workflowId", exec.getWorkflowId(),
                "runId", exec.getRunId()
        ));
    }

    @PostMapping("/checkout/{orderId}/address")
    public ResponseEntity<Void> provideAddressTest(@PathVariable UUID orderId,
                                                   @RequestHeader("X-User-Id") UUID userId,
                                                   @RequestBody Address address
    ) {
        orderService.assertOrderBelongsToBuyer(orderId, userId);
        auctionCheckoutStarter.sendAddressProvided(orderId, address);
        return ResponseEntity.ok().build();
    }
//
//    @PostMapping("/checkout/{orderId}/payment")
//    public ResponseEntity<Void> authorizePayment(@PathVariable UUID orderId) {
//        auctionCheckoutStarter.sendPaymentAuthorized(orderId, "TEST");
//        return ResponseEntity.ok().build();
//        // TODO: remove
//    }

    @PostMapping("/checkout/{orderId}/cancel")
    public ResponseEntity<Void> cancelCheckout(@PathVariable UUID orderId,
                                               @RequestHeader("X-User-Id") UUID userId
    ) {
        orderService.assertOrderBelongsToBuyer(orderId, userId);
        orderService.cancelCheckout(orderId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/checkout/{orderId}/status")
    public ResponseEntity<CheckoutStatusResponse> getListingCreationStatus(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID orderId) {

        orderService.assertOrderBelongsToBuyer(orderId, userId);

        String workflowId = orderService.getWorkflowIdByOrderId(orderId);

        try {
            WorkflowStub workflowStub = workflowClient.newUntypedWorkflowStub(workflowId);
            var description = workflowStub.describe();
            var workflowStatus = description.getStatus();

            switch (workflowStatus) {
                case WORKFLOW_EXECUTION_STATUS_COMPLETED:
                    // Get the actual result to determine business success
                    try {
                        WorkflowResult result = workflowStub.getResult(
                                1, TimeUnit.SECONDS, WorkflowResult.class
                        );

                        return ResponseEntity.ok(new CheckoutStatusResponse(
                                result.isSuccess() ? "COMPLETED" : "FAILED",
                                result.getStatus(),
                                result.getErrorMessage()
                        ));
                    } catch (TimeoutException e) {
                        // Shouldn't happen since workflow is completed
                        return ResponseEntity.ok(new CheckoutStatusResponse("COMPLETED", null, null));
                    }

                case WORKFLOW_EXECUTION_STATUS_FAILED:
                case WORKFLOW_EXECUTION_STATUS_TERMINATED:
                case WORKFLOW_EXECUTION_STATUS_TIMED_OUT:
                case WORKFLOW_EXECUTION_STATUS_CANCELED:
                    return ResponseEntity.ok(new CheckoutStatusResponse("FAILED", null, null));

                case WORKFLOW_EXECUTION_STATUS_RUNNING:
                case WORKFLOW_EXECUTION_STATUS_CONTINUED_AS_NEW:
                default:
                    return ResponseEntity.ok(new CheckoutStatusResponse("RUNNING", null, null));
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
