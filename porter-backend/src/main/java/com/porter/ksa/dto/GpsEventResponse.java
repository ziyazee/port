package com.porter.ksa.dto;

import com.porter.ksa.entity.GpsEventType;
import java.time.Instant;

public record GpsEventResponse(
        Long id,
        Long trackingSessionId,
        GpsEventType eventType,
        Double latitude,
        Double longitude,
        String notes,
        Instant eventAt,
        Instant createdAt
) {
}
