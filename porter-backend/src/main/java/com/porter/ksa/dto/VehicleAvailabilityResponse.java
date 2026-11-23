package com.porter.ksa.dto;

import com.porter.ksa.entity.VehicleType;

public record VehicleAvailabilityResponse(
        VehicleType vehicleType,
        long availableCount,
        long assignedCount,
        long maintenanceCount,
        long inactiveCount
) {
}
