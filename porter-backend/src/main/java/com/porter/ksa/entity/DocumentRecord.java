package com.porter.ksa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "documents")
public class DocumentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentOwnerType ownerType;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 80)
    private String documentType;

    @Column(nullable = false, length = 500)
    private String storageKey;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String uploadedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected DocumentRecord() {
    }

    public DocumentRecord(
            DocumentOwnerType ownerType,
            Long ownerId,
            String documentType,
            String storageKey,
            String originalFilename,
            String uploadedBy
    ) {
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.documentType = documentType;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.uploadedBy = uploadedBy;
    }

    @PrePersist
    void beforeCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public DocumentOwnerType getOwnerType() {
        return ownerType;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
