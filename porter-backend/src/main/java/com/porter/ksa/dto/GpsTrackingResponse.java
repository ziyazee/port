package com.porter.ksa.dto;

import com.porter.ksa.entity.GpsProviderType;
import java.time.Instant;

public record GpsTrackingResponse(
        Long id,
        Long gpsDeviceId,
        GpsProviderType provider,
        String providerVehicleId,
        String vehicleFleetNumber,
        double latitude,
        double longitude,
        double speedKph,
        double headingDegrees,
        Instant recordedAt,
        Instant receivedAt
) {
}
