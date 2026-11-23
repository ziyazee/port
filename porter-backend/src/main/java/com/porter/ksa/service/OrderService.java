package com.porter.ksa.service;

import com.porter.ksa.dto.OrderCreateRequest;
import com.porter.ksa.dto.OrderResponse;
import com.porter.ksa.dto.OrderStatusUpdateRequest;
import com.porter.ksa.dto.OrderUpdateRequest;
import com.porter.ksa.entity.CustomerAccount;
import com.porter.ksa.entity.OrderStatus;
import com.porter.ksa.entity.OrderVehicleStatus;
import com.porter.ksa.entity.TransportOrder;
import com.porter.ksa.repository.CustomerAccountRepository;
import com.porter.ksa.repository.OrderVehicleRepository;
import com.porter.ksa.repository.TransportOrderRepository;
import com.porter.ksa.repository.TrackingSessionRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class OrderService {

    private final TransportOrderRepository orders;
    private final CustomerAccountRepository customerAccounts;
    private final OrderVehicleRepository orderVehicles;
    private final TrackingSessionRepository trackingSessions;
    private final TrackingLifecycleService lifecycleService;

    public OrderService(
            TransportOrderRepository orders,
            CustomerAccountRepository customerAccounts,
            OrderVehicleRepository orderVehicles,
            TrackingSessionRepository trackingSessions,
            TrackingLifecycleService lifecycleService
    ) {
        this.orders = orders;
        this.customerAccounts = customerAccounts;
        this.orderVehicles = orderVehicles;
        this.trackingSessions = trackingSessions;
        this.lifecycleService = lifecycleService;
    }

    public OrderResponse create(OrderCreateRequest request) {
        CustomerAccount customerAccount = findCustomer(request.customerAccountId());
        validateCoordinatePair(request.pickupLatitude(), request.pickupLongitude(), "Pickup");
        validateCoordinatePair(request.dropLatitude(), request.dropLongitude(), "Drop");

        TransportOrder order = new TransportOrder(
                customerAccount,
                requireText(request.fromLocation(), "From location is required"),
                requireText(request.toLocation(), "To location is required"),
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.dropLatitude(),
                request.dropLongitude(),
                request.vehicleType(),
                requireText(request.loadDescription(), "Load description is required"),
                request.cargoSpecification(),
                trimToNull(request.cargoHeight()),
                trimToNull(request.cargoWeight()),
                trimToNull(request.cargoDimensions()),
                request.quantityUnits(),
                requireText(request.createdBy(), "Created by is required")
        );

        return toResponse(orders.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orders.findAllWithCustomerAccount()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return toResponse(findOrder(id));
    }

    public OrderResponse update(Long id, OrderUpdateRequest request) {
        TransportOrder order = findOrder(id);
        if (order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed or cancelled orders cannot be updated");
        }
        validateCoordinatePair(request.pickupLatitude(), request.pickupLongitude(), "Pickup");
        validateCoordinatePair(request.dropLatitude(), request.dropLongitude(), "Drop");

        order.updateDetails(
                findCustomer(request.customerAccountId()),
                requireText(request.fromLocation(), "From location is required"),
                requireText(request.toLocation(), "To location is required"),
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.dropLatitude(),
                request.dropLongitude(),
                request.vehicleType(),
                requireText(request.loadDescription(), "Load description is required"),
                request.cargoSpecification(),
                trimToNull(request.cargoHeight()),
                trimToNull(request.cargoWeight()),
                trimToNull(request.cargoDimensions()),
                request.quantityUnits(),
                requireText(request.updatedBy(), "Updated by is required")
        );

        return toResponse(orders.save(order));
    }

    public OrderResponse verify(Long id, OrderStatusUpdateRequest request) {
        TransportOrder order = findOrder(id);
        if (order.getOrderStatus() != OrderStatus.CREATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only created orders can be verified");
        }

        order.updateStatus(OrderStatus.VERIFIED, requireText(request.updatedBy(), "Updated by is required"));
        return toResponse(orders.save(order));
    }

    public OrderResponse cancel(Long id, OrderStatusUpdateRequest request) {
        TransportOrder order = findOrder(id);
        if (order.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed orders cannot be cancelled");
        }

        String updatedBy = requireText(request.updatedBy(), "Updated by is required");
        orderVehicles.findByOrder_IdOrderByIdAsc(id)
                .stream()
                .filter(assignment -> assignment.getStatus() != OrderVehicleStatus.DELIVERED)
                .filter(assignment -> assignment.getStatus() != OrderVehicleStatus.CANCELLED)
                .forEach(assignment -> trackingSessions.findByOrderVehicle_Id(assignment.getId())
                        .ifPresentOrElse(
                                session -> lifecycleService.applyManualTransition(
                                        session.getId(),
                                        com.porter.ksa.entity.TrackingStatus.CANCELLED,
                                        updatedBy
                                ),
                                () -> lifecycleService.applyManualTransition(
                                        assignment,
                                        OrderVehicleStatus.CANCELLED,
                                        updatedBy
                                )
                        ));
        order.updateStatus(OrderStatus.CANCELLED, updatedBy);
        return toResponse(orders.save(order));
    }

    public OrderResponse complete(Long id, String updatedBy) {
        TransportOrder order = findOrder(id);
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancelled orders cannot be completed");
        }

        order.updateStatus(OrderStatus.COMPLETED, requireText(updatedBy, "Updated by is required"));
        return toResponse(orders.save(order));
    }

    public OrderResponse markVehicleAssigned(Long id, String updatedBy) {
        TransportOrder order = findOrder(id);
        if (order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is not assignable");
        }
        if (order.getOrderStatus() == OrderStatus.CREATED || order.getOrderStatus() == OrderStatus.VERIFIED) {
            order.updateStatus(OrderStatus.VEHICLE_ASSIGNED, requireText(updatedBy, "Updated by is required"));
            orders.save(order);
        }

        return toResponse(order);
    }

    TransportOrder findOrderEntity(Long id) {
        return findOrder(id);
    }

    TransportOrder findOrderEntityForUpdate(Long id) {
        return orders.findByIdWithCustomerAccountForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    void markVehicleAssigned(TransportOrder order, String updatedBy) {
        if (order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is not assignable");
        }
        if (order.getOrderStatus() == OrderStatus.CREATED || order.getOrderStatus() == OrderStatus.VERIFIED) {
            order.updateStatus(OrderStatus.VEHICLE_ASSIGNED, requireText(updatedBy, "Updated by is required"));
        }
    }

    public OrderResponse toResponse(TransportOrder order) {
        CustomerAccount customerAccount = order.getCustomerAccount();
        return new OrderResponse(
                order.getId(),
                customerAccount.getId(),
                customerAccount.getCompanyName(),
                order.getFromLocation(),
                order.getToLocation(),
                order.getPickupLatitude(),
                order.getPickupLongitude(),
                order.getDropLatitude(),
                order.getDropLongitude(),
                order.getVehicleType(),
                order.getLoadDescription(),
                order.getCargoSpecification(),
                order.getCargoHeight(),
                order.getCargoWeight(),
                order.getCargoDimensions(),
                order.getQuantityUnits(),
                orderVehicles.countByOrder_Id(order.getId()),
                order.getOrderStatus(),
                order.getCreatedAt(),
                order.getCreatedBy(),
                order.getUpdatedAt(),
                order.getUpdatedBy()
        );
    }

    private TransportOrder findOrder(Long id) {
        return orders.findByIdWithCustomerAccount(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private CustomerAccount findCustomer(Long customerAccountId) {
        return customerAccounts.findById(customerAccountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected company does not exist"));
    }

    private String requireText(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private void validateCoordinatePair(Double latitude, Double longitude, String label) {
        if ((latitude == null) != (longitude == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    label + " latitude and longitude must be provided together"
            );
        }
    }
}
