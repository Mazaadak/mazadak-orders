package com.mazadak.orders.service.impl;

import com.mazadak.orders.client.CartClient;
import com.mazadak.orders.client.UserClient;
import com.mazadak.orders.dto.request.CheckoutRequest;
import com.mazadak.orders.dto.request.OrderFilterDto;
import com.mazadak.orders.dto.response.OrderResponse;
import com.mazadak.orders.exception.ResourceNotFoundException;
import com.mazadak.orders.mapper.OrderMapper;
import com.mazadak.orders.model.entity.Order;
import com.mazadak.orders.model.enumeration.OrderStatus;
import com.mazadak.orders.repository.OrderRepository;
import com.mazadak.orders.repository.specification.OrderSpecifications;
import com.mazadak.orders.service.OrderService;
import com.mazadak.orders.workflow.FixedPriceCheckoutWorkflow;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final CartClient cartClient;
    private final WorkflowClient workflowClient;


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
        FixedPriceCheckoutWorkflow workflow = workflowClient.newWorkflowStub(
                FixedPriceCheckoutWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue("ORDER_TASK_QUEUE")
                        .setWorkflowId("checkout-" + idempotencyKey)
                        .build()
        );
        // TODO: Need to handle running workflow exception
        WorkflowExecution exec = WorkflowClient.start(workflow::processCheckout, request);
        log.info("Workflow started: workflowId={}, runId={}", exec.getWorkflowId(), exec.getRunId());
        return exec;
    }

    @Override
    public void markCompleted(UUID orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId.toString()));

        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
    }

    @Override
    public void markFailed(UUID orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId.toString()));

        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);
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
