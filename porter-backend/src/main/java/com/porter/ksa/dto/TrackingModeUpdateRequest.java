package com.porter.ksa.dto;

import com.porter.ksa.entity.TrackingMode;
import jakarta.validation.constraints.NotNull;

public record TrackingModeUpdateRequest(
        @NotNull(message = "Tracking mode is required")
        TrackingMode mode
) {
}
