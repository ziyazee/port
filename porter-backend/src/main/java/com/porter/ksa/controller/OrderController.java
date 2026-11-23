package com.porter.ksa.controller;

import com.porter.ksa.dto.OrderCreateRequest;
import com.porter.ksa.dto.OrderResponse;
import com.porter.ksa.dto.OrderStatusUpdateRequest;
import com.porter.ksa.dto.OrderUpdateRequest;
import com.porter.ksa.service.OrderCreationService;
import com.porter.ksa.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderCreationService orderCreationService;

    public OrderController(OrderService orderService, OrderCreationService orderCreationService) {
        this.orderService = orderService;
        this.orderCreationService = orderCreationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderCreateRequest request) {
        return orderCreationService.create(request);
    }

    @GetMapping
    public List<OrderResponse> findAll() {
        return orderService.findAll();
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @PutMapping("/{id}")
    public OrderResponse update(@PathVariable Long id, @Valid @RequestBody OrderUpdateRequest request) {
        return orderService.update(id, request);
    }

    @PatchMapping("/{id}/verify")
    public OrderResponse verify(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest request) {
        return orderService.verify(id, request);
    }

    @PatchMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest request) {
        return orderService.cancel(id, request);
    }
}
