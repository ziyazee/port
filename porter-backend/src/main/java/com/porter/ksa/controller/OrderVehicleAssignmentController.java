package com.porter.ksa.controller;

import com.porter.ksa.dto.OrderVehicleAssignRequest;
import com.porter.ksa.dto.OrderVehicleResponse;
import com.porter.ksa.dto.OrderVehicleStatusUpdateRequest;
import com.porter.ksa.service.OrderVehicleAssignmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OrderVehicleAssignmentController {

    private final OrderVehicleAssignmentService assignmentService;

    public OrderVehicleAssignmentController(OrderVehicleAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping("/orders/{orderId}/vehicles")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderVehicleResponse assign(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderVehicleAssignRequest request
    ) {
        return assignmentService.assign(orderId, request);
    }

    @GetMapping("/orders/{orderId}/vehicles")
    public List<OrderVehicleResponse> findByOrder(@PathVariable Long orderId) {
        return assignmentService.findByOrder(orderId);
    }

    @PatchMapping("/order-vehicles/{id}/status")
    public OrderVehicleResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderVehicleStatusUpdateRequest request
    ) {
        return assignmentService.updateStatus(id, request);
    }
}
