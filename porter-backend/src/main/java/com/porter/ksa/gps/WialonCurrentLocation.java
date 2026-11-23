package com.porter.ksa.gps;

import com.porter.ksa.entity.GpsProviderType;
import java.time.Instant;

public record WialonCurrentLocation(
        Long wialonUnitId,
        String unitName,
        double latitude,
        double longitude,
        double speedKph,
        double headingDegrees,
        Instant recordedAt,
        Instant receivedAt
) {

    public GpsLocationSnapshot toGpsLocationSnapshot() {
        return new GpsLocationSnapshot(
                GpsProviderType.WIALON,
                String.valueOf(wialonUnitId),
                latitude,
                longitude,
                speedKph,
                headingDegrees,
                recordedAt
        );
    }
}
