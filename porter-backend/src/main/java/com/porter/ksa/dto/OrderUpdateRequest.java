package com.porter.ksa.dto;

import com.porter.ksa.entity.CargoSpecification;
import com.porter.ksa.entity.VehicleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record OrderUpdateRequest(
        @NotNull(message = "Company is required")
        Long customerAccountId,

        @NotBlank(message = "From location is required")
        @Size(max = 500, message = "From location must be 500 characters or fewer")
        String fromLocation,

        @NotBlank(message = "To location is required")
        @Size(max = 500, message = "To location must be 500 characters or fewer")
        String toLocation,

        @DecimalMin(value = "-90.0", message = "Pickup latitude must be at least -90")
        @DecimalMax(value = "90.0", message = "Pickup latitude must be at most 90")
        Double pickupLatitude,

        @DecimalMin(value = "-180.0", message = "Pickup longitude must be at least -180")
        @DecimalMax(value = "180.0", message = "Pickup longitude must be at most 180")
        Double pickupLongitude,

        @DecimalMin(value = "-90.0", message = "Drop latitude must be at least -90")
        @DecimalMax(value = "90.0", message = "Drop latitude must be at most 90")
        Double dropLatitude,

        @DecimalMin(value = "-180.0", message = "Drop longitude must be at least -180")
        @DecimalMax(value = "180.0", message = "Drop longitude must be at most 180")
        Double dropLongitude,

        @NotNull(message = "Vehicle type is required")
        VehicleType vehicleType,

        @NotBlank(message = "Load description is required")
        @Size(max = 1000, message = "Load description must be 1000 characters or fewer")
        String loadDescription,

        @NotNull(message = "Cargo specification is required")
        CargoSpecification cargoSpecification,

        @Size(max = 80, message = "Cargo height must be 80 characters or fewer")
        String cargoHeight,

        @Size(max = 80, message = "Cargo weight must be 80 characters or fewer")
        String cargoWeight,

        @Size(max = 120, message = "Cargo dimensions must be 120 characters or fewer")
        String cargoDimensions,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantityUnits,

        @NotBlank(message = "Updated by is required")
        @Size(max = 120, message = "Updated by must be 120 characters or fewer")
        String updatedBy
) {
}
