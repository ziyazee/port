package com.porter.ksa.dto;

import com.porter.ksa.entity.GpsProviderType;
import java.time.Instant;

public record GpsDeviceResponse(
        Long id,
        GpsProviderType provider,
        String providerVehicleId,
        String vehicleFleetNumber,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
