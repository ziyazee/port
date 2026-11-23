package com.porter.ksa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderStatusUpdateRequest(
        @NotBlank(message = "Updated by is required")
        @Size(max = 120, message = "Updated by must be 120 characters or fewer")
        String updatedBy
) {
}
