package com.mazadak.orders.controller;

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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AuctionCheckoutStarter auctionCheckoutStarter;

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

    @PostMapping("/test/create")
    public ResponseEntity<Order> createTestOrder(@RequestBody CreateTestOrderRequest request) {
        Order order = new Order();
        order.setBuyerId(request.getBuyerId());
        order.setType(request.getType());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);

        if (request.getType() == OrderType.FIXED_PRICE) {
            order.setCartId(request.getCartId());
        } else {
            order.setAuctionId(request.getAuctionId());
        }

        // Create shipping address
        Address address = new Address();
        address.setStreet(request.getShippingAddress().getStreet());
        address.setCity(request.getShippingAddress().getCity());
        address.setState(request.getShippingAddress().getState());
        address.setPostalCode(request.getShippingAddress().getZipCode());
        address.setCountry(request.getShippingAddress().getCountry());
        order.setShippingAddress(address);

        // Save order first to get ID
        order = orderRepository.save(order);

        // Create order items
        BigDecimal total = BigDecimal.ZERO;
        for (TestOrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setProductId(itemRequest.getProductId());
            item.setProductName(itemRequest.getProductName());
            item.setProductImageUrl(itemRequest.getProductImageUrl());
            item.setUnitPrice(itemRequest.getUnitPrice());
            item.setQuantity(itemRequest.getQuantity());
            item.setOrder(order);

            orderItemRepository.save(item);

            BigDecimal subtotal = itemRequest.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            total = total.add(subtotal);
        }

        // Update total amount
        order.setTotalAmount(total);
        order = orderRepository.save(order);

        return ResponseEntity.ok(order);
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        var response = orderService.checkout(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout/{orderId}/address")
    public ResponseEntity<Void> provideAddressTest(@PathVariable UUID orderId) {
        auctionCheckoutStarter.sendAddressProvided(orderId, new Address(
                "test",
                "test",
                "test",
                "test",
                "test")
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/checkout/{orderId}/payment")
    public ResponseEntity<Void> authorizePayment(@PathVariable UUID orderId) {
        auctionCheckoutStarter.sendPaymentAuthorized(orderId, "TEST");
        return ResponseEntity.ok().build();
    }
}
