package com.mazadak.orders.service.impl;

import com.mazadak.orders.client.AuctionClient;
import com.mazadak.orders.client.CartClient;
import com.mazadak.orders.client.UserClient;
import com.mazadak.orders.dto.client.CartResponseDTO;
import com.mazadak.orders.dto.request.CheckoutRequest;
import com.mazadak.orders.dto.request.OrderFilterDto;
import com.mazadak.orders.dto.response.OrderResponse;
import com.mazadak.orders.exception.CartOwnershipException;
import com.mazadak.orders.exception.ResourceNotFoundException;
import com.mazadak.orders.mapper.OrderMapper;
import com.mazadak.orders.model.entity.Order;
import com.mazadak.orders.model.enumeration.OrderStatus;
import com.mazadak.orders.model.enumeration.OrderType;
import com.mazadak.orders.model.enumeration.PaymentStatus;
import com.mazadak.orders.repository.OrderRepository;
import com.mazadak.orders.repository.specification.OrderSpecifications;
import com.mazadak.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final CartClient cartClient;
    private final AuctionClient auctionClient;

    @Override
    public OrderResponse getOrderById(UUID id) {
        var order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id.toString()));

        return OrderMapper.toResponse(order);
    }

    @Override
    public Page<OrderResponse> findOrdersByCriteria(OrderFilterDto filter, Pageable pageable) {
        Specification<Order> specification = OrderSpecifications.buildFromFilter(filter);
        return orderRepository.findAll(specification, pageable)
                .map(OrderMapper::toResponse);
    }

    @Override
    public OrderResponse checkout(CheckoutRequest request) {
        // TODO: user existence validation
//        UserProfileResponse user = userClient.getUserProfile(request.userId()).getBody();
//        if (user == null) {
//            throw new ResourceNotFoundException("User", "id", request.userId().toString());
//        }

        CartResponseDTO cart = cartClient.getCart(request.userId()).getBody();
        if (cart == null || !cart.cartId().equals(request.cartId())) {
            throw new CartOwnershipException("Cart with id %s doesn't belong to user with id %s", request.cartId(), request.userId());
        }


        // construct order object
        // buyerId, type, totalAmount, status, address, orderItems, paymentIntentId, auctionId, cartId
        // construct order items
        // productId, productName, productImageUrl, unitPrice, quantity, subtotal
        // save record
        // start workflow
        return new OrderResponse(UUID.randomUUID(), request.userId(), OrderType.FIXED_PRICE, BigDecimal.ZERO, OrderStatus.PENDING, request.address(), PaymentStatus.PENDING, null, null, null, request.cartId());
    }

    @Override
    public void markCompleted(UUID orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId.toString()));

        order.setStatus(OrderStatus.COMPLETED);
    }

    @Override
    public void markFailed(UUID orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId.toString()));

        order.setStatus(OrderStatus.FAILED);
    }// TODO: clean up

    @Override
    public void createOrderForWinner(UUID auctionId, UUID bidderId) {
        // construct & persist order record
        // product client
        // payment client
        // users client (fix)
        // worker
        // configure bindings for auction completed and invalid
        // figure out email sending for checkout for winner
        // starter
    }
}
