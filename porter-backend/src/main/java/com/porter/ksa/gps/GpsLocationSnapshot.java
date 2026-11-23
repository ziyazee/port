package com.porter.ksa.gps;

import com.porter.ksa.entity.GpsProviderType;
import java.time.Instant;

public record GpsLocationSnapshot(
        GpsProviderType provider,
        String providerVehicleId,
        double latitude,
        double longitude,
        double speedKph,
        double headingDegrees,
        Instant recordedAt
) {
}
