package com.porter.ksa.dto;

import java.time.Instant;

public record TrackingLocationUpdateResponse(
        Long trackingSessionId,
        Long orderVehicleId,
        Long vehicleId,
        String fleetNumber,
        double latitude,
        double longitude,
        Double speedKph,
        Double headingDegrees,
        Instant lastSeenAt
) {
}
