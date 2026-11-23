package com.porter.ksa.service;

import com.porter.ksa.dto.OrderCreateRequest;
import com.porter.ksa.dto.OrderResponse;
import com.porter.ksa.dto.OrderVehicleAssignRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCreationService {

    private final OrderService orderService;
    private final OrderVehicleAssignmentService assignmentService;

    public OrderCreationService(
            OrderService orderService,
            OrderVehicleAssignmentService assignmentService
    ) {
        this.orderService = orderService;
        this.assignmentService = assignmentService;
    }

    @Transactional
    public OrderResponse create(OrderCreateRequest request) {
        OrderResponse order = orderService.create(request);
        List<Long> vehicleIds = request.vehicleIds() == null
                ? List.of()
                : request.vehicleIds().stream().distinct().toList();

        for (Long vehicleId : vehicleIds) {
            assignmentService.assign(
                    order.id(),
                    new OrderVehicleAssignRequest(vehicleId, null, request.createdBy())
            );
        }

        return vehicleIds.isEmpty() ? order : orderService.findById(order.id());
    }
}
