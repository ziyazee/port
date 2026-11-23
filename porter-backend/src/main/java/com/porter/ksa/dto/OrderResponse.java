package com.porter.ksa.dto;

import com.porter.ksa.entity.CargoSpecification;
import com.porter.ksa.entity.OrderStatus;
import com.porter.ksa.entity.VehicleType;
import java.time.Instant;

public record OrderResponse(
        Long id,
        Long customerAccountId,
        String companyName,
        String fromLocation,
        String toLocation,
        Double pickupLatitude,
        Double pickupLongitude,
        Double dropLatitude,
        Double dropLongitude,
        VehicleType vehicleType,
        String loadDescription,
        CargoSpecification cargoSpecification,
        String cargoHeight,
        String cargoWeight,
        String cargoDimensions,
        int quantityUnits,
        long assignedVehicles,
        OrderStatus orderStatus,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy
) {
}
