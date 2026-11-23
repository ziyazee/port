package com.porter.ksa.dto;

import com.porter.ksa.entity.DocumentOwnerType;
import java.time.Instant;

public record DocumentRecordResponse(
        Long id,
        DocumentOwnerType ownerType,
        Long ownerId,
        String documentType,
        String storageKey,
        String originalFilename,
        String uploadedBy,
        Instant createdAt
) {
}
