package com.porter.ksa.dto;

import com.porter.ksa.entity.GpsProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GpsDeviceCreateRequest(
        @NotNull(message = "GPS provider is required")
        GpsProviderType provider,

        @NotBlank(message = "Provider vehicle id is required")
        @Size(max = 120, message = "Provider vehicle id must be 120 characters or fewer")
        String providerVehicleId,

        @Size(max = 80, message = "Fleet number must be 80 characters or fewer")
        String vehicleFleetNumber
) {
}
