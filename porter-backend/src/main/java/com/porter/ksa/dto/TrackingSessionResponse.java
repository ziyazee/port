package com.porter.ksa.dto;

import com.porter.ksa.entity.TrackingMode;
import com.porter.ksa.entity.TrackingStatus;
import java.time.Instant;

public record TrackingSessionResponse(
        Long id,
        Long orderVehicleId,
        Long orderId,
        Long vehicleId,
        String fleetNumber,
        TrackingMode mode,
        TrackingStatus status,
        double pickupLatitude,
        double pickupLongitude,
        double dropLatitude,
        double dropLongitude,
        int geofenceRadiusMeters,
        Instant startedAt,
        Instant stoppedAt,
        Instant updatedAt
) {
}
