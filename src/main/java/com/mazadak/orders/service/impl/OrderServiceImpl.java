package com.mazadak.orders.service.impl;

import com.mazadak.orders.client.AuctionClient;
import com.mazadak.orders.dto.client.AuctionResponse;
import com.mazadak.orders.dto.request.CheckoutRequest;
import com.mazadak.orders.dto.request.OrderFilterDto;
import com.mazadak.orders.dto.response.OrderResponse;
import com.mazadak.orders.exception.ResourceNotFoundException;
import com.mazadak.orders.mapper.OrderMapper;
import com.mazadak.orders.model.entity.Address;
import com.mazadak.orders.model.entity.Order;
import com.mazadak.orders.model.enumeration.OrderStatus;
import com.mazadak.orders.model.enumeration.OrderType;
import com.mazadak.orders.model.enumeration.PaymentStatus;
import com.mazadak.orders.repository.OrderRepository;
import com.mazadak.orders.repository.specification.OrderSpecifications;
import com.mazadak.orders.service.OrderService;
import com.mazadak.orders.dto.internal.AuctionCheckoutRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
//    private final UserClient userClient;
//    private final CartClient cartClient;
    private final AuctionClient auctionClient;
//    private final ProductClient productClient;
    private final RepositoryMethodInvocationListener repositoryMethodInvocationListener;

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

//        CartResponseDTO cart = cartClient.getCart(request.userId()).getBody();
//        if (cart == null || !cart.cartId().equals(request.cartId())) {
//            throw new CartOwnershipException("Cart with id %s doesn't belong to user with id %s", request.cartId(), request.userId());
//        }


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
        setStatus(orderId, OrderStatus.COMPLETED);
    }

    @Override
    public void markFailed(UUID orderId) {
        setStatus(orderId, OrderStatus.FAILED);
    }

    @Override
    public void markCancelled(UUID orderId) {
        setStatus(orderId, OrderStatus.CANCELLED);
    }

    private void setStatus(UUID orderId, OrderStatus newStatus) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId.toString()));

        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    @Override
    public UUID createOrderForWinner(AuctionResponse auction, AuctionCheckoutRequest.BidderInfo bidder) {
        var order = new Order();
        order.setBuyerId(bidder.id());
        order.setType(OrderType.AUCTION);
        order.setTotalAmount(bidder.amount());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setAuctionId(auction.id());

        var saved = orderRepository.save(order);
        return saved.getId();
    }

    @Override
    public void setAddress(UUID orderId, Address address) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId.toString()));

        order.setShippingAddress(address);
        orderRepository.save(order);
    }

    @Override
    public void setPaymentIntentId(UUID orderId, String paymentIntentId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId.toString()));

        order.setPaymentIntentId(paymentIntentId);
        orderRepository.save(order);
    }

    @Override
    public void setPaymentStatus(UUID orderId, PaymentStatus status) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId.toString()));

        order.setPaymentStatus(status);
        orderRepository.save(order);
    }
}
