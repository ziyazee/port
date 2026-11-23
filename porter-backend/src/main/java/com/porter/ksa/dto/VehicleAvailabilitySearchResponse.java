package com.porter.ksa.dto;

import com.porter.ksa.entity.VehicleStatus;
import com.porter.ksa.entity.VehicleType;
import java.time.Instant;

public record VehicleAvailabilitySearchResponse(
        Long vehicleId,
        String fleetNumber,
        String plateNumber,
        VehicleType vehicleType,
        VehicleStatus status,
        String availabilityGroup,
        Double currentLatitude,
        Double currentLongitude,
        Double distanceKm,
        Integer estimatedMinutesToPickup,
        Instant lastSeenAt
) {
}
