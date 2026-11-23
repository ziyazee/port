package com.porter.ksa.dto;

import com.porter.ksa.entity.GpsProviderType;
import com.porter.ksa.entity.VehicleStatus;
import com.porter.ksa.entity.VehicleType;
import java.time.Instant;
import java.util.List;

public record VehicleResponse(
        Long id,
        String fleetNumber,
        String plateNumber,
        VehicleType vehicleType,
        String driverName,
        VehicleStatus status,
        GpsProviderType gpsProvider,
        String gpsProviderVehicleId,
        Double currentLatitude,
        Double currentLongitude,
        Double currentSpeedKph,
        Double currentHeadingDegrees,
        Instant lastSeenAt,
        Instant createdAt,
        Instant updatedAt,
        List<VehicleDriverAssignmentResponse> driverHistory
) {
}
