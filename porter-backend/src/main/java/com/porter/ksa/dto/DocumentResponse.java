package com.porter.ksa.dto;

public record DocumentResponse(
        String storageKey,
        String originalFilename,
        String contentType,
        Long sizeBytes
) {
}
