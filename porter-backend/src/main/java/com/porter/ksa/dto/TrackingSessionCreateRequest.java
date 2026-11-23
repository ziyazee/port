package com.porter.ksa.dto;

import com.porter.ksa.entity.TrackingMode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TrackingSessionCreateRequest(
        @NotNull(message = "Order vehicle id is required")
        Long orderVehicleId,

        TrackingMode mode,

        @DecimalMin(value = "-90.0", message = "Pickup latitude must be greater than or equal to -90")
        @DecimalMax(value = "90.0", message = "Pickup latitude must be less than or equal to 90")
        double pickupLatitude,

        @DecimalMin(value = "-180.0", message = "Pickup longitude must be greater than or equal to -180")
        @DecimalMax(value = "180.0", message = "Pickup longitude must be less than or equal to 180")
        double pickupLongitude,

        @DecimalMin(value = "-90.0", message = "Drop latitude must be greater than or equal to -90")
        @DecimalMax(value = "90.0", message = "Drop latitude must be less than or equal to 90")
        double dropLatitude,

        @DecimalMin(value = "-180.0", message = "Drop longitude must be greater than or equal to -180")
        @DecimalMax(value = "180.0", message = "Drop longitude must be less than or equal to 180")
        double dropLongitude,

        @Min(value = 50, message = "Geofence radius must be at least 50 meters")
        @Max(value = 5000, message = "Geofence radius must be 5000 meters or less")
        int geofenceRadiusMeters
) {
}
