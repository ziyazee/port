package com.porter.ksa.storage;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.google.auth.oauth2.GoogleCredentials;
import com.porter.ksa.config.DocumentStorageProperties;
import com.porter.ksa.config.GoogleCloudStorageProperties;
import com.porter.ksa.entity.DocumentOwnerType;
import com.porter.ksa.entity.DocumentReference;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
@ConditionalOnProperty(name = "porter.document-storage.provider", havingValue = "gcs")
public class GoogleCloudDocumentStorage implements DocumentStorage {

    private final DocumentStorageProperties documentProperties;
    private final GoogleCloudStorageProperties gcsProperties;
    private final Storage storage;

    public GoogleCloudDocumentStorage(
            DocumentStorageProperties documentProperties,
            GoogleCloudStorageProperties gcsProperties
    ) {
        this.documentProperties = documentProperties;
        this.gcsProperties = gcsProperties;
        this.storage = createStorageClient(gcsProperties);
    }

    @Override
    public DocumentReference store(Long customerAccountId, DocumentType documentType, MultipartFile file) {
        validate(file);

        String contentType = file.getContentType();
        String objectName = objectName(customerAccountId, documentType);
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(requireBucketName(), objectName))
                .setContentType(contentType)
                .build();

        try {
            storage.create(blobInfo, file.getBytes());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read document", exception);
        } catch (StorageException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not upload document", exception);
        }

        return new DocumentReference(objectName, file.getOriginalFilename(), contentType, file.getSize());
    }

    @Override
    public DocumentReference store(DocumentOwnerType ownerType, Long ownerId, String documentType, MultipartFile file) {
        validate(file);

        String contentType = file.getContentType();
        String objectName = genericObjectName(ownerType, ownerId, documentType, file.getOriginalFilename());
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(requireBucketName(), objectName))
                .setContentType(contentType)
                .build();

        try {
            storage.create(blobInfo, file.getBytes());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read document", exception);
        } catch (StorageException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not upload document", exception);
        }

        return new DocumentReference(objectName, file.getOriginalFilename(), contentType, file.getSize());
    }

    private Storage createStorageClient(GoogleCloudStorageProperties properties) {
        StorageOptions.Builder builder = StorageOptions.newBuilder();
        if (hasText(properties.getProjectId())) {
            builder.setProjectId(properties.getProjectId().trim());
        }

        if (hasText(properties.getCredentialsPath())) {
            builder.setCredentials(loadCredentials(properties.getCredentialsPath()));
        }

        return builder.build().getService();
    }

    private GoogleCredentials loadCredentials(String credentialsPath) {
        Path path = Path.of(credentialsPath.trim());
        try (InputStream inputStream = Files.newInputStream(path)) {
            return GoogleCredentials.fromStream(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Google Cloud credentials from configured path", exception);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required document is missing");
        }

        if (file.getSize() > documentProperties.getMaxSizeBytes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document must be 10MB or smaller");
        }

        if (!documentProperties.getAllowedContentTypes().contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document must be a PDF, JPG, or PNG");
        }
    }

    private String objectName(Long customerAccountId, DocumentType documentType) {
        return "customer-documents/" + customerAccountId + "/" + documentType.storageFilename();
    }

    private String genericObjectName(
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

    private String requireBucketName() {
        if (!hasText(gcsProperties.getBucketName())) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Google Cloud Storage bucket is not configured"
            );
        }

        return gcsProperties.getBucketName().trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
