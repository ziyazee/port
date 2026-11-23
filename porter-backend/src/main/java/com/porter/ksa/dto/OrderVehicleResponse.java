package com.porter.ksa.dto;

import com.porter.ksa.entity.OrderVehicleStatus;
import com.porter.ksa.entity.VehicleType;
import java.time.Instant;

public record OrderVehicleResponse(
        Long id,
        Long orderId,
        Long vehicleId,
        String fleetNumber,
        String plateNumber,
        VehicleType vehicleType,
        Long driverId,
        String driverName,
        OrderVehicleStatus status,
        Instant assignedAt,
        Instant deliveredAt,
        Instant createdAt,
        Instant updatedAt
) {
}
