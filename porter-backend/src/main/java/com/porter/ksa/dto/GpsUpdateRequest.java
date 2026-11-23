package com.porter.ksa.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record GpsUpdateRequest(
        @NotBlank(message = "Vehicle fleet number is required")
        @Size(max = 80, message = "Vehicle fleet number must be 80 characters or fewer")
        String vehicleFleetNumber,

        @DecimalMin(value = "-90.0", message = "Latitude must be greater than or equal to -90")
        @DecimalMax(value = "90.0", message = "Latitude must be less than or equal to 90")
        double latitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be greater than or equal to -180")
        @DecimalMax(value = "180.0", message = "Longitude must be less than or equal to 180")
        double longitude,

        @PositiveOrZero(message = "Speed must be zero or greater")
        double speedKph,

        @DecimalMin(value = "0.0", message = "Heading must be greater than or equal to 0")
        @DecimalMax(value = "360.0", message = "Heading must be less than or equal to 360")
        double headingDegrees,

        Instant recordedAt
) {
}
