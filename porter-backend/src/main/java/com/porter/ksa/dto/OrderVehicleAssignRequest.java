package com.porter.ksa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderVehicleAssignRequest(
        @NotNull(message = "Vehicle id is required")
        Long vehicleId,

        Long driverId,

        @NotBlank(message = "Assigned by is required")
        @Size(max = 120, message = "Assigned by must be 120 characters or fewer")
        String assignedBy
) {
}
