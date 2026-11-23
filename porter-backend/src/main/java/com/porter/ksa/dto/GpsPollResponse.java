package com.porter.ksa.dto;

import java.time.Instant;

public record GpsPollResponse(
        int devicesPolled,
        int locationsUpdated,
        Instant polledAt
) {
}
