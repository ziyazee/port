package com.porter.ksa.dto;

import jakarta.validation.constraints.NotNull;

public record DriverAssignVehicleRequest(
        @NotNull(message = "Vehicle id is required")
        Long vehicleId
) {
}
