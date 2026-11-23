package com.porter.ksa.service;

import com.porter.ksa.dto.OrderVehicleAssignRequest;
import com.porter.ksa.dto.OrderVehicleResponse;
import com.porter.ksa.dto.OrderVehicleStatusUpdateRequest;
import com.porter.ksa.entity.Driver;
import com.porter.ksa.entity.DriverStatus;
import com.porter.ksa.entity.OrderStatus;
import com.porter.ksa.entity.OrderVehicle;
import com.porter.ksa.entity.OrderVehicleStatus;
import com.porter.ksa.entity.TrackingStatus;
import com.porter.ksa.entity.TransportOrder;
import com.porter.ksa.entity.Vehicle;
import com.porter.ksa.entity.VehicleStatus;
import com.porter.ksa.repository.DriverRepository;
import com.porter.ksa.repository.OrderVehicleRepository;
import com.porter.ksa.repository.TrackingSessionRepository;
import com.porter.ksa.repository.VehicleRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class OrderVehicleAssignmentService {

    private final OrderVehicleRepository orderVehicles;
    private final VehicleRepository vehicles;
    private final DriverRepository drivers;
    private final OrderService orderService;
    private final TrackingSessionRepository trackingSessions;
    private final TrackingSessionService trackingSessionService;
    private final TrackingLifecycleService lifecycleService;

    public OrderVehicleAssignmentService(
            OrderVehicleRepository orderVehicles,
            VehicleRepository vehicles,
            DriverRepository drivers,
            OrderService orderService,
            TrackingSessionRepository trackingSessions,
            TrackingSessionService trackingSessionService,
            TrackingLifecycleService lifecycleService
    ) {
        this.orderVehicles = orderVehicles;
        this.vehicles = vehicles;
        this.drivers = drivers;
        this.orderService = orderService;
        this.trackingSessions = trackingSessions;
        this.trackingSessionService = trackingSessionService;
        this.lifecycleService = lifecycleService;
    }

    public OrderVehicleResponse assign(Long orderId, OrderVehicleAssignRequest request) {
        TransportOrder order = orderService.findOrderEntityForUpdate(orderId);
        if (order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is not assignable");
        }

        Vehicle vehicle = vehicles.findById(request.vehicleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only available vehicles can be assigned");
        }
        if (orderVehicles.existsByOrder_IdAndVehicle_Id(orderId, vehicle.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle is already assigned to this order");
        }

        Driver driver = resolveVehicleDriver(vehicle, request.driverId());

        vehicle.updateDriverName(driver == null ? vehicle.getDriverName() : driver.getFullName());
        vehicle.update(
                vehicle.getFleetNumber(),
                vehicle.getPlateNumber(),
                vehicle.getVehicleType(),
                VehicleStatus.ASSIGNED,
                vehicle.getDriverName()
        );

        OrderVehicle assignment = orderVehicles.save(new OrderVehicle(order, vehicle, driver));
        orderService.markVehicleAssigned(order, request.assignedBy());
        trackingSessionService.startAutomatically(assignment);

        return toResponse(assignment);
    }

    private Driver resolveVehicleDriver(Vehicle vehicle, Long requestedDriverId) {
        Driver assignedDriver = drivers.findByCurrentVehicle_Id(vehicle.getId()).orElse(null);
        if (requestedDriverId != null) {
            Driver requestedDriver = drivers.findById(requestedDriverId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
            if (requestedDriver.getCurrentVehicle() == null
                    || !requestedDriver.getCurrentVehicle().getId().equals(vehicle.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Driver must already be assigned to the selected vehicle"
                );
            }
            assignedDriver = requestedDriver;
        }

        if (assignedDriver != null) {
            if (assignedDriver.getStatus() == DriverStatus.BLOCKED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Blocked driver cannot be assigned");
            }
            if (assignedDriver.getStatus() == DriverStatus.OFFLINE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Offline driver cannot be assigned");
            }
            assignedDriver.startOrder();
            return assignedDriver;
        }

        if (vehicle.getDriverName() == null || vehicle.getDriverName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vehicle must have a driver assigned before it can be assigned to an order"
            );
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<OrderVehicleResponse> findByOrder(Long orderId) {
        return orderVehicles.findByOrder_IdOrderByIdAsc(orderId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public OrderVehicleResponse updateStatus(Long id, OrderVehicleStatusUpdateRequest request) {
        OrderVehicle assignment = orderVehicles.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order vehicle assignment not found"));

        trackingSessions.findByOrderVehicle_Id(id)
                .ifPresentOrElse(
                        session -> lifecycleService.applyManualTransition(
                                session.getId(),
                                TrackingStatus.valueOf(request.status().name()),
                                request.updatedBy()
                        ),
                        () -> lifecycleService.applyManualTransition(assignment, request.status(), request.updatedBy())
                );
        return toResponse(orderVehicles.findById(id).orElseThrow());
    }

    private OrderVehicleResponse toResponse(OrderVehicle assignment) {
        Vehicle vehicle = assignment.getVehicle();
        Driver driver = assignment.getDriver();
        String driverName = driver == null ? vehicle.getDriverName() : driver.getFullName();
        return new OrderVehicleResponse(
                assignment.getId(),
                assignment.getOrder().getId(),
                vehicle.getId(),
                vehicle.getFleetNumber(),
                vehicle.getPlateNumber(),
                vehicle.getVehicleType(),
                driver == null ? null : driver.getId(),
                driverName,
                assignment.getStatus(),
                assignment.getAssignedAt(),
                assignment.getDeliveredAt(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }
}
