package com.porter.ksa.dto;

import com.porter.ksa.entity.WaitingPenaltyStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record WaitingPenaltyResponse(
        Long id,
        Long trackingSessionId,
        WaitingPenaltyStatus status,
        Instant unloadingStartedAt,
        Instant freeUntil,
        Instant penaltyStartedAt,
        Instant pausedAt,
        Instant waivedAt,
        BigDecimal manualAdjustmentAmount,
        String notes,
        Instant updatedAt
) {
}
