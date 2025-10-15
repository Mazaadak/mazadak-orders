package com.mazadak.orders.service.impl;

import com.mazadak.orders.dto.request.OrderFilterDto;
import com.mazadak.orders.dto.response.OrderResponse;
import com.mazadak.orders.exception.ResourceNotFoundException;
import com.mazadak.orders.mapper.OrderMapper;
import com.mazadak.orders.model.entity.Order;
import com.mazadak.orders.repository.OrderRepository;
import com.mazadak.orders.repository.specification.OrderSpecifications;
import com.mazadak.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;

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
    public OrderResponse createOrder(Order order) {
        orderRepository.save(order);
        return OrderMapper.toResponse(order);
    }
}
