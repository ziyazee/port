package com.porter.ksa.dto;

import java.time.Instant;

public record VehicleDriverAssignmentResponse(
        Long id,
        String driverName,
        Instant assignedAt,
        Instant unassignedAt
) {
}
