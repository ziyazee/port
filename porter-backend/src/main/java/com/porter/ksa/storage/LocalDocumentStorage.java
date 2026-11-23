package com.porter.ksa.storage;

import com.porter.ksa.config.DocumentStorageProperties;
import com.porter.ksa.entity.DocumentOwnerType;
import com.porter.ksa.entity.DocumentReference;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
@ConditionalOnProperty(name = "porter.document-storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalDocumentStorage implements DocumentStorage {

    private final DocumentStorageProperties properties;
    private final Path basePath;

    public LocalDocumentStorage(DocumentStorageProperties properties) {
        this.properties = properties;
        this.basePath = Path.of(properties.getBasePath());
    }

    @Override
    public DocumentReference store(Long customerAccountId, DocumentType documentType, MultipartFile file) {
        validate(file);

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String storageKey = storageKey(customerAccountId, documentType);
        Path target = basePath.resolve(storageKey).normalize();

        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store document", exception);
        }

        return new DocumentReference(storageKey, originalFilename, contentType, file.getSize());
    }

    @Override
    public DocumentReference store(DocumentOwnerType ownerType, Long ownerId, String documentType, MultipartFile file) {
        validate(file);

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String storageKey = genericStorageKey(ownerType, ownerId, documentType, originalFilename);
        Path target = basePath.resolve(storageKey).normalize();

        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store document", exception);
        }

        return new DocumentReference(storageKey, originalFilename, contentType, file.getSize());
    }

    private String storageKey(Long customerAccountId, DocumentType documentType) {
        return "customer-documents/" + customerAccountId + "/" + documentType.storageFilename();
    }

    private String genericStorageKey(
            DocumentOwnerType ownerType,
            Long ownerId,
            String documentType,
            String originalFilename
    ) {
        String ownerPath = ownerType.name().toLowerCase(Locale.ROOT).replace('_', '-');
        String safeType = sanitize(documentType);
        String safeFilename = sanitize(originalFilename == null ? "document" : originalFilename);
        String uniquePart = Instant.now().toEpochMilli() + "-" + UUID.randomUUID();
        return "documents/" + ownerPath + "/" + ownerId + "/" + safeType + "/" + uniquePart + "-" + safeFilename;
    }

    private String sanitize(String value) {
        return value.trim()
                .replaceAll("[^A-Za-z0-9._-]", "-")
                .replaceAll("-+", "-");
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required document is missing");
        }

        if (file.getSize() > properties.getMaxSizeBytes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document must be 10MB or smaller");
        }

        if (!properties.getAllowedContentTypes().contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document must be a PDF, JPG, or PNG");
        }
    }

}
