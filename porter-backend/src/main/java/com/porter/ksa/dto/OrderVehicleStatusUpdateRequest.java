package com.porter.ksa.dto;

import com.porter.ksa.entity.OrderVehicleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderVehicleStatusUpdateRequest(
        @NotNull(message = "Order vehicle status is required")
        OrderVehicleStatus status,

        @NotBlank(message = "Updated by is required")
        @Size(max = 120, message = "Updated by must be 120 characters or fewer")
        String updatedBy
) {
}
