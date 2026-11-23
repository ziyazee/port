package com.porter.ksa.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record WaitingPenaltyManualActionRequest(
        BigDecimal adjustmentAmount,

        @Size(max = 1000, message = "Notes must be 1000 characters or fewer")
        String notes
) {
}
