package com.mazadak.orders.service.impl;

import com.mazadak.common.exception.shared.ResourceNotFoundException;
import com.mazadak.orders.client.ProductClient;
import com.mazadak.orders.dto.client.AuctionResponse;
import com.mazadak.orders.dto.client.CartItemResponseDTO;
import com.mazadak.orders.dto.client.CartResponseDTO;
import com.mazadak.orders.dto.request.CheckoutRequest;
import com.mazadak.orders.dto.request.OrderFilterDto;
import com.mazadak.orders.dto.response.OrderResponse;
import com.mazadak.orders.dto.response.ProductResponseDTO;
import com.mazadak.orders.exception.AmountTooLargeException;
import com.mazadak.orders.mapper.OrderMapper;
import com.mazadak.orders.model.entity.Address;
import com.mazadak.orders.model.entity.Order;
import com.mazadak.orders.model.entity.OrderItem;
import com.mazadak.orders.model.enumeration.OrderStatus;
import com.mazadak.orders.model.enumeration.OrderType;
import com.mazadak.orders.model.enumeration.PaymentStatus;
import com.mazadak.orders.repository.OrderItemRepository;
import com.mazadak.orders.repository.OrderRepository;
import com.mazadak.orders.repository.specification.OrderSpecifications;
import com.mazadak.orders.service.OrderService;
import com.mazadak.orders.dto.internal.AuctionCheckoutRequest;
import com.mazadak.orders.workflow.starter.AuctionCheckoutStarter;
import com.mazadak.orders.workflow.starter.FixedPriceCheckoutStarter;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final FixedPriceCheckoutStarter fixedPriceCheckoutStarter;
    private final AuctionCheckoutStarter auctionCheckoutStarter;
    private final OrderItemRepository orderItemRepository;


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
    public WorkflowExecution checkout(UUID idempotencyKey, CheckoutRequest request) {
        log.info("Checkout request: {}", request);
        return fixedPriceCheckoutStarter.startFixedPriceCheckout(idempotencyKey, request);
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

        var orderItem = new OrderItem(
                auction.productId(),
                auction.sellerId(),
                auction.title(),
                bidder.amount(),
                1,
                bidder.amount(),
                order
        );
        order.setOrderItems(List.of(orderItem));

//        orderItemRepository.save(orderItem);
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

    @Override
    public OrderResponse createFixedPriceOrder(CheckoutRequest request, CartResponseDTO cart, UUID idempotencyKey) {

        Order order = new Order();
        order.setBuyerId(request.userId());
        order.setType(OrderType.FIXED_PRICE);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setShippingAddress(request.address());
        order.setCartId(cart.cartId());
        order.setIdempotencyKey(idempotencyKey);

        log.info("Getting products details for order {}", order.getId());
        List<UUID> productIds = cart.cartItems().stream()
                .map(CartItemResponseDTO::productId)
                .toList();

        List<ProductResponseDTO> products = productClient.getProductsByIds(productIds).getBody();

        // Create a map of product IDs to product details for fast lookup
        Map<UUID, ProductResponseDTO> productMap = products.stream()
                .collect(Collectors.toMap(ProductResponseDTO::productId, Function.identity()));

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItemResponseDTO cartItem : cart.cartItems()) {
            log.info("Mapping cart item {} to order item", cartItem.productId());
            ProductResponseDTO product = productMap.get(cartItem.productId());
            if (product == null) {
                log.error("Product not found: {}", cartItem.productId());
                throw new RuntimeException("Product not found: " + cartItem.productId());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.productId());
            orderItem.setProductName(product.title());
            orderItem.setUnitPrice(product.price());
            orderItem.setSellerId(product.sellerId());
            orderItem.setQuantity(cartItem.quantity());
            orderItem.setOrder(order);

            BigDecimal itemTotal = product.price().multiply(new BigDecimal(cartItem.quantity()));
            orderItem.setSubtotal(itemTotal);
            totalAmount = totalAmount.add(itemTotal);

            orderItems.add(orderItem);
            log.info("Added order item {} to order {}", orderItem.getId(), order.getId());
        }

        log.info("Setting order total amount to {}", totalAmount);
        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        log.info("Saving order {}", order);
        Order createdOrder = orderRepository.save(order);

        log.info("Order saved successfully");

        return OrderMapper.toResponse(createdOrder);
    }

    @Override
    public void assertOrderBelongsToBuyer(UUID orderId, UUID userId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "Id", orderId.toString()));

        if (!userId.equals(order.getBuyerId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    String.format(
                            "User %s not authorized to work with order %s",
                            userId,
                            order
                    )
            );
        }
    }

    @Override
    public String getWorkflowIdByOrderId(UUID orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "Id", orderId.toString()));

        return getWorkflowIdForOrder(order);
    }

    @Override
    public void authorizePayment(UUID orderId, String paymentIntentId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "Id", orderId.toString()));

        var id = getWorkflowIdForOrder(order);

        if (order.getType() == OrderType.FIXED_PRICE) {
            fixedPriceCheckoutStarter.sendPaymentAuthorized(
                    order.getIdempotencyKey(),
                    orderId,
                    paymentIntentId
            );
        } else {
            auctionCheckoutStarter.sendPaymentAuthorized(
                    orderId,
                    order.getAuctionId(),
                    paymentIntentId
            );
        }
    }

    @Override
    public void attachIntent(UUID orderId, String paymentIntentId, String clientSecret) {
        log.info("Attaching intent {} to order {}", paymentIntentId, orderId);
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "Id", orderId.toString()));

        var id = getWorkflowIdForOrder(order);

        if (order.getType() == OrderType.FIXED_PRICE) {
            fixedPriceCheckoutStarter.sendIntentCreated(
                    order.getIdempotencyKey(),
                    orderId,
                    paymentIntentId,
                    clientSecret
            );
        } else {
            auctionCheckoutStarter.sendIntentCreated(
                    orderId,
                    order.getAuctionId(),
                    paymentIntentId,
                    clientSecret
            );
        }
    }

    @Override
    public void setClientSecret(UUID orderId, String clientSecret) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId.toString()));

        order.setClientSecret(clientSecret);
        orderRepository.save(order);
    }

    @Override
    public void assertAmountNotTooLarge(UUID orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId.toString()));

        if (order.getTotalAmount().compareTo(BigDecimal.valueOf(999999)) > 0) {
            throw new AmountTooLargeException(orderId, order.getTotalAmount());
        }
    }

    @Override
    public String getWorkflowIdForOrder(Order order) {
        return order.getType() == OrderType.FIXED_PRICE ?
                "fixed-price-checkout-" + order.getIdempotencyKey() :
                "auction-checkout-" + order.getAuctionId();
    }

    @Override
    public void cancelCheckout(UUID orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "Id", orderId.toString()));

        var id = getWorkflowIdForOrder(order);

        if (order.getType() == OrderType.FIXED_PRICE) {
            fixedPriceCheckoutStarter.sendCheckoutCancelled(
                    order.getIdempotencyKey(),
                    orderId,
                    "User cancelled checkout"
            );
        } else {
            auctionCheckoutStarter.sendCheckoutCancelled(
                    orderId,
                    order.getAuctionId(),
                    "User cancelled checkout"
            );
        }
    }
}
