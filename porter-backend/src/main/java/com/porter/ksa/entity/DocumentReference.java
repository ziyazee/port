package com.porter.ksa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class DocumentReference {

    @Column(length = 500)
    private String storageKey;

    private String originalFilename;

    @Column(length = 120)
    private String contentType;

    private Long sizeBytes;

    protected DocumentReference() {
    }

    public DocumentReference(String storageKey, String originalFilename, String contentType, long sizeBytes) {
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }
}
