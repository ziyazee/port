package com.porter.ksa.dto;

import com.porter.ksa.entity.DriverStatus;
import java.time.Instant;

public record DriverResponse(
        Long id,
        String fullName,
        String phoneNumber,
        String nationalId,
        String licenseNumber,
        DriverStatus status,
        Long currentVehicleId,
        String currentVehicleFleetNumber,
        String currentVehiclePlateNumber,
        Instant createdAt,
        Instant updatedAt
) {
}
