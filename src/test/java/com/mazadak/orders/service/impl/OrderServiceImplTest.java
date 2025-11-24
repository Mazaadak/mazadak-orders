package com.mazadak.orders.service.impl;

import com.mazadak.orders.client.ProductClient;
import com.mazadak.orders.dto.client.AuctionResponse;
import com.mazadak.orders.dto.client.CartItemResponseDTO;
import com.mazadak.orders.dto.client.CartResponseDTO;
import com.mazadak.orders.dto.internal.AuctionCheckoutRequest;
import com.mazadak.orders.dto.request.CheckoutRequest;
import com.mazadak.orders.dto.request.OrderFilterDto;
import com.mazadak.orders.dto.response.OrderResponse;
import com.mazadak.orders.dto.response.ProductResponseDTO;
import com.mazadak.orders.exception.AmountTooLargeException;
import com.mazadak.orders.exception.ResourceNotFoundException;
import com.mazadak.orders.model.entity.Address;
import com.mazadak.orders.model.entity.Order;
import com.mazadak.orders.model.enumeration.OrderStatus;
import com.mazadak.orders.model.enumeration.OrderType;
import com.mazadak.orders.model.enumeration.PaymentStatus;
import com.mazadak.orders.repository.OrderItemRepository;
import com.mazadak.orders.repository.OrderRepository;
import com.mazadak.orders.workflow.starter.AuctionCheckoutStarter;
import com.mazadak.orders.workflow.starter.FixedPriceCheckoutStarter;
import io.temporal.api.common.v1.WorkflowExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Tests")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private FixedPriceCheckoutStarter fixedPriceCheckoutStarter;

    @Mock
    private AuctionCheckoutStarter auctionCheckoutStarter;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID orderId;
    private UUID userId;
    private UUID productId;
    private UUID itemId;
    private UUID sellerId;
    private UUID idempotencyKey;
    private Order order;
    private Address address;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        itemId  = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        idempotencyKey = UUID.randomUUID();

        address = new Address();
        address.setStreet("123 Abdelrahman Mostafa St");
        address.setCity("Cairo");
        address.setCountry("Egypt");
        address.setPostalCode("12345");

        order = new Order();
        order.setId(orderId);
        order.setBuyerId(userId);
        order.setType(OrderType.FIXED_PRICE);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalAmount(BigDecimal.valueOf(100));
        order.setIdempotencyKey(idempotencyKey);
        order.setOrderItems(new ArrayList<>());
    }

    @Nested
    @DisplayName("GetOrderById Tests")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Should return order when found")
        void shouldReturnOrderWhenFound() {
            // Arrange
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act
            OrderResponse result = orderService.getOrderById(orderId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(orderId);
            assertThat(result.buyerId()).isEqualTo(userId);
            verify(orderRepository).findById(orderId);
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            // Arrange
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.getOrderById(orderId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Order")
                    .hasMessageContaining(orderId.toString());
        }
    }

    @Nested
    @DisplayName("FindOrdersByCriteria Tests")
    class FindOrdersByCriteriaTests {

        @Test
        @DisplayName("Should return paginated orders matching criteria")
        void shouldReturnPaginatedOrdersMatchingCriteria() {
            // Arrange
            OrderFilterDto filter = new OrderFilterDto(userId,null, null, null,null,null, null, null, null);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(order));

            when(orderRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(orderPage);

            // Act
            Page<OrderResponse> result = orderService.findOrdersByCriteria(filter, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().id()).isEqualTo(orderId);
            verify(orderRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Should return empty page when no orders match criteria")
        void shouldReturnEmptyPageWhenNoOrdersMatchCriteria() {
            // Arrange
            OrderFilterDto filter = new OrderFilterDto(userId,null, null, null,null,null, null, null, null);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> emptyPage = new PageImpl<>(List.of());

            when(orderRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(emptyPage);

            // Act
            Page<OrderResponse> result = orderService.findOrdersByCriteria(filter, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Checkout Tests")
    class CheckoutTests {

        @Test
        @DisplayName("Should start fixed price checkout workflow")
        void shouldStartFixedPriceCheckoutWorkflow() {
            // Arrange
            CheckoutRequest request = new CheckoutRequest(userId, address);
            WorkflowExecution execution = WorkflowExecution.newBuilder()
                    .setWorkflowId("workflow-123")
                    .setRunId("run-123")
                    .build();

            when(fixedPriceCheckoutStarter.startFixedPriceCheckout(idempotencyKey, request))
                    .thenReturn(execution);

            // Act
            WorkflowExecution result = orderService.checkout(idempotencyKey, request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getWorkflowId()).isEqualTo("workflow-123");
            verify(fixedPriceCheckoutStarter).startFixedPriceCheckout(idempotencyKey, request);
        }
    }

    @Nested
    @DisplayName("Order Status Management Tests")
    class OrderStatusManagementTests {

        @Test
        @DisplayName("Should mark order as completed")
        void shouldMarkOrderAsCompleted() {
            // Arrange
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            // Act
            orderService.markCompleted(orderId);

            // Assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("Should mark order as failed")
        void shouldMarkOrderAsFailed() {
            // Arrange
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            // Act
            orderService.markFailed(orderId);

            // Assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("Should mark order as cancelled")
        void shouldMarkOrderAsCancelled() {
            // Arrange
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            // Act
            orderService.markCancelled(orderId);

            // Assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("Should throw exception when marking status on non-existent order")
        void shouldThrowExceptionWhenMarkingStatusOnNonExistentOrder() {
            // Arrange
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.markCompleted(orderId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("CreateOrderForWinner Tests")
    class CreateOrderForWinnerTests {

        @Test
        @DisplayName("Should create auction order for winner")
        void shouldCreateAuctionOrderForWinner() {
            // Arrange
            UUID auctionId = UUID.randomUUID();
            BigDecimal startingPrice = BigDecimal.valueOf(500);

            AuctionResponse auction = new AuctionResponse(
                    auctionId,
                    productId,
                    sellerId,
                    "Auction Product",
                    startingPrice,
                    null, null, null, null, null,null
            );

            AuctionCheckoutRequest.BidderInfo bidder = new AuctionCheckoutRequest.BidderInfo(
                    userId,
                    startingPrice
            );

            Order savedOrder = new Order();
            savedOrder.setId(orderId);

            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

            // Act
            UUID result = orderService.createOrderForWinner(auction, bidder);

            // Assert
            assertThat(result).isEqualTo(orderId);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());

            Order capturedOrder = orderCaptor.getValue();
            assertThat(capturedOrder.getBuyerId()).isEqualTo(userId);
            assertThat(capturedOrder.getType()).isEqualTo(OrderType.AUCTION);
            assertThat(capturedOrder.getTotalAmount()).isEqualTo(startingPrice);
            assertThat(capturedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(capturedOrder.getAuctionId()).isEqualTo(auctionId);
            assertThat(capturedOrder.getOrderItems()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("SetAddress Tests")
    class SetAddressTests {

        @Test
        @DisplayName("Should set shipping address on order")
        void shouldSetShippingAddressOnOrder() {
            // Arrange
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            // Act
            orderService.setAddress(orderId, address);

            // Assert
            assertThat(order.getShippingAddress()).isEqualTo(address);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            // Arrange
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.setAddress(orderId, address))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Payment Management Tests")
    class PaymentManagementTests {

        @Test
        @DisplayName("Should set payment intent ID")
        void shouldSetPaymentIntentId() {
            // Arrange
            String paymentIntentId = "123456";
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            // Act
            orderService.setPaymentIntentId(orderId, paymentIntentId);

            // Assert
            assertThat(order.getPaymentIntentId()).isEqualTo(paymentIntentId);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("Should set payment status")
        void shouldSetPaymentStatus() {
            // Arrange
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            // Act
            orderService.setPaymentStatus(orderId, PaymentStatus.AUTHORIZED);

            // Assert
            assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("Should set client secret")
        void shouldSetClientSecret() {
            // Arrange
            String clientSecret = "123456";
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            // Act
            orderService.setClientSecret(orderId, clientSecret);

            // Assert
            assertThat(order.getClientSecret()).isEqualTo(clientSecret);
            verify(orderRepository).save(order);
        }
    }

    @Nested
    @DisplayName("CreateFixedPriceOrder Tests")
    class CreateFixedPriceOrderTests {

        @Test
        @DisplayName("Should create fixed price order from cart")
        void shouldCreateFixedPriceOrderFromCart() {
            // Arrange
            UUID cartId = UUID.randomUUID();
            CheckoutRequest request = new CheckoutRequest(userId, address);

            CartItemResponseDTO cartItem = new CartItemResponseDTO(itemId, productId, 2);
            CartResponseDTO cart = new CartResponseDTO(
                    cartId,
                    userId,
                    List.of(cartItem)
            );

            ProductResponseDTO product = new ProductResponseDTO(
                    productId,
                    sellerId,
                    "Test Product",
                    "Description",
                    BigDecimal.valueOf(50)
            );

            Order savedOrder = new Order();
            savedOrder.setId(orderId);
            savedOrder.setBuyerId(userId);
            savedOrder.setTotalAmount(BigDecimal.valueOf(100));

            when(productClient.getProductsByIds(anyList()))
                    .thenReturn(ResponseEntity.ok(List.of(product)));
            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

            // Act
            OrderResponse result = orderService.createFixedPriceOrder(request, cart, idempotencyKey);

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());

            Order capturedOrder = orderCaptor.getValue();
            assertThat(capturedOrder.getBuyerId()).isEqualTo(userId);
            assertThat(capturedOrder.getType()).isEqualTo(OrderType.FIXED_PRICE);
            assertThat(capturedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(capturedOrder.getShippingAddress()).isEqualTo(address);
            assertThat(capturedOrder.getCartId()).isEqualTo(cartId);
            assertThat(capturedOrder.getIdempotencyKey()).isEqualTo(idempotencyKey);
            assertThat(capturedOrder.getOrderItems()).hasSize(1);
            assertThat(capturedOrder.getTotalAmount()).isEqualTo(BigDecimal.valueOf(100));
        }

        @Test
        @DisplayName("Should throw exception when product not found")
        void shouldThrowExceptionWhenProductNotFound() {
            // Arrange
            UUID cartId = UUID.randomUUID();

            CheckoutRequest request = new CheckoutRequest(userId, address);

            CartItemResponseDTO cartItem = new CartItemResponseDTO(itemId, productId, 2);
            CartResponseDTO cart = new CartResponseDTO(
                    cartId,
                    userId,
                    List.of(cartItem)
            );

            when(productClient.getProductsByIds(anyList()))
                    .thenReturn(ResponseEntity.ok(List.of()));

            // Act & Assert
            assertThatThrownBy(() -> orderService.createFixedPriceOrder(request, cart, idempotencyKey))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Product not found");
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should pass when order belongs to buyer")
        void shouldPassWhenOrderBelongsToBuyer() {
            // Arrange
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act & Assert
            orderService.assertOrderBelongsToBuyer(orderId, userId);
            verify(orderRepository).findById(orderId);
        }

        @Test
        @DisplayName("Should throw exception when order does not belong to buyer")
        void shouldThrowExceptionWhenOrderDoesNotBelongToBuyer() {
            // Arrange
            UUID User2Id = UUID.randomUUID();
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act & Assert
            assertThatThrownBy(() -> orderService.assertOrderBelongsToBuyer(orderId, User2Id))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("not authorized");
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            // Arrange
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.assertOrderBelongsToBuyer(orderId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Amount Validation Tests")
    class AmountValidationTests {

        @Test
        @DisplayName("Should pass when amount is within limit")
        void shouldPassWhenAmountIsWithinLimit() {
            // Arrange
            order.setTotalAmount(BigDecimal.valueOf(100000));
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act & Assert
            orderService.assertAmountNotTooLarge(orderId);
        }

        @Test
        @DisplayName("Should throw exception when amount exceeds limit")
        void shouldThrowExceptionWhenAmountExceedsLimit() {
            // Arrange
            order.setTotalAmount(BigDecimal.valueOf(1000000));
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act & Assert
            assertThatThrownBy(() -> orderService.assertAmountNotTooLarge(orderId))
                    .isInstanceOf(AmountTooLargeException.class);
        }
    }

    @Nested
    @DisplayName("Workflow Management Tests")
    class WorkflowManagementTests {

        @Test
        @DisplayName("Should return correct workflow ID for fixed price order")
        void shouldReturnCorrectWorkflowIdForFixedPriceOrder() {
            // Arrange

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act
            String result = orderService.getWorkflowIdByOrderId(orderId);

            // Assert
            assertThat(result).isEqualTo("fixed-price-checkout-" + idempotencyKey);
        }

        @Test
        @DisplayName("Should return correct workflow ID for auction order")
        void shouldReturnCorrectWorkflowIdForAuctionOrder() {
            // Arrange
            UUID auctionId = UUID.randomUUID();
            order.setType(OrderType.AUCTION);
            order.setAuctionId(auctionId);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act
            String result = orderService.getWorkflowIdByOrderId(orderId);

            // Assert
            assertThat(result).isEqualTo("auction-checkout-" + auctionId);
        }

    }

    @Nested
    @DisplayName("Payment Authorization Tests")
    class PaymentAuthorizationTests {

        @Test
        @DisplayName("Should authorize payment for fixed price order")
        void shouldAuthorizePaymentForFixedPriceOrder() {
            // Arrange
            String paymentIntentId = "123456";
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act
            orderService.authorizePayment(orderId, paymentIntentId);

            // Assert
            verify(fixedPriceCheckoutStarter).sendPaymentAuthorized(
                    idempotencyKey,
                    orderId,
                    paymentIntentId
            );
            verify(auctionCheckoutStarter, never()).sendPaymentAuthorized(any(), any(), any());
        }

        @Test
        @DisplayName("Should authorize payment for auction order")
        void shouldAuthorizePaymentForAuctionOrder() {
            // Arrange
            UUID auctionId = UUID.randomUUID();
            String paymentIntentId = "123456";
            order.setType(OrderType.AUCTION);
            order.setAuctionId(auctionId);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act
            orderService.authorizePayment(orderId, paymentIntentId);

            // Assert
            verify(auctionCheckoutStarter).sendPaymentAuthorized(
                    orderId,
                    auctionId,
                    paymentIntentId
            );
            verify(fixedPriceCheckoutStarter, never()).sendPaymentAuthorized(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Attach Intent Tests")
    class AttachIntentTests {

        @Test
        @DisplayName("Should attach intent for fixed price order")
        void shouldAttachIntentForFixedPriceOrder() {
            // Arrange
            String paymentIntentId = "123456";
            String clientSecret = "Hello my friend I'm Alien-X";
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act
            orderService.attachIntent(orderId, paymentIntentId, clientSecret);

            // Assert
            verify(fixedPriceCheckoutStarter).sendIntentCreated(
                    idempotencyKey,
                    orderId,
                    paymentIntentId,
                    clientSecret
            );
        }

        @Test
        @DisplayName("Should attach intent for auction order")
        void shouldAttachIntentForAuctionOrder() {
            // Arrange
            UUID auctionId = UUID.randomUUID();
            String paymentIntentId = "123456";
            String clientSecret = "Hello my friend I'm Abdelrahman Mostafa aka the greatest programmer in the world";
            order.setType(OrderType.AUCTION);
            order.setAuctionId(auctionId);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act
            orderService.attachIntent(orderId, paymentIntentId, clientSecret);

            // Assert
            verify(auctionCheckoutStarter).sendIntentCreated(
                    orderId,
                    auctionId,
                    paymentIntentId,
                    clientSecret
            );
        }
    }

    @Nested
    @DisplayName("Cancel Checkout Tests")
    class CancelCheckoutTests {

        @Test
        @DisplayName("Should cancel fixed price checkout")
        void shouldCancelFixedPriceCheckout() {
            // Arrange
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act
            orderService.cancelCheckout(orderId);

            // Assert
            verify(fixedPriceCheckoutStarter).sendCheckoutCancelled(
                    idempotencyKey,
                    orderId,
                    "User cancelled checkout"
            );
            verify(auctionCheckoutStarter, never()).sendCheckoutCancelled(any(), any(), any());
        }

        @Test
        @DisplayName("Should cancel auction checkout")
        void shouldCancelAuctionCheckout() {
            // Arrange
            UUID auctionId = UUID.randomUUID();
            order.setType(OrderType.AUCTION);
            order.setAuctionId(auctionId);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // Act
            orderService.cancelCheckout(orderId);

            // Assert
            verify(auctionCheckoutStarter).sendCheckoutCancelled(
                    orderId,
                    auctionId,
                    "User cancelled checkout"
            );
            verify(fixedPriceCheckoutStarter, never()).sendCheckoutCancelled(any(), any(), any());
        }

        @Test
        @DisplayName("Should throw exception when cancelling non-existent order")
        void shouldThrowExceptionWhenCancellingNonExistentOrder() {
            // Arrange
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.cancelCheckout(orderId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}