package com.porter.ksa.dto;

public record GoogleStorageTestResponse(
        String bucketName,
        String objectName,
        String originalFilename,
        String contentType,
        long sizeBytes
) {
}
