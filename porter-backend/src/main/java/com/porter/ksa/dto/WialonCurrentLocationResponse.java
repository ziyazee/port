package com.porter.ksa.dto;

import com.porter.ksa.entity.GpsProviderType;
import java.time.Instant;

public record WialonCurrentLocationResponse(
        GpsProviderType provider,
        Long wialonUnitId,
        String unitName,
        Double latitude,
        Double longitude,
        Double speedKph,
        Double headingDegrees,
        Instant recordedAt,
        Instant receivedAt,
        MapDetailsResponse mapDetails
) {
}
