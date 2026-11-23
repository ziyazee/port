package com.porter.ksa.dto;

import com.porter.ksa.entity.DriverStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DriverCreateRequest(
        @NotBlank(message = "Driver name is required")
        @Size(max = 160, message = "Driver name must be 160 characters or fewer")
        String fullName,

        @NotBlank(message = "Driver phone number is required")
        @Pattern(regexp = "^(?:\\+9665|05)\\d{8}$", message = "Driver phone number must be a valid KSA mobile number")
        String phoneNumber,

        @Size(max = 80, message = "National id must be 80 characters or fewer")
        String nationalId,

        @Size(max = 80, message = "License number must be 80 characters or fewer")
        String licenseNumber,

        @NotNull(message = "Driver status is required")
        DriverStatus status
) {
}
