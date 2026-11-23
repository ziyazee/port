package com.porter.ksa.dto;

import com.porter.ksa.entity.GpsProviderType;
import com.porter.ksa.entity.VehicleStatus;
import com.porter.ksa.entity.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VehicleCreateRequest(
        @NotBlank(message = "Fleet number is required")
        @Size(max = 80, message = "Fleet number must be 80 characters or fewer")
        String fleetNumber,

        @NotBlank(message = "Plate number is required")
        @Size(max = 80, message = "Plate number must be 80 characters or fewer")
        String plateNumber,

        @NotNull(message = "Vehicle type is required")
        VehicleType vehicleType,

        @NotBlank(message = "Driver name is required")
        @Size(max = 160, message = "Driver name must be 160 characters or fewer")
        String driverName,

        @NotNull(message = "Vehicle status is required")
        VehicleStatus status,

        GpsProviderType gpsProvider,

        @Size(max = 120, message = "GPS provider vehicle id must be 120 characters or fewer")
        String gpsProviderVehicleId
) {
}
