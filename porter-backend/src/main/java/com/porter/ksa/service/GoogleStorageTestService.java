package com.porter.ksa.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.porter.ksa.config.DocumentStorageProperties;
import com.porter.ksa.config.GoogleCloudStorageProperties;
import com.porter.ksa.dto.GoogleStorageTestResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GoogleStorageTestService {

    private final DocumentStorageProperties documentStorageProperties;
    private final GoogleCloudStorageProperties googleCloudStorageProperties;

    public GoogleStorageTestService(
            DocumentStorageProperties documentStorageProperties,
            GoogleCloudStorageProperties googleCloudStorageProperties
    ) {
        this.documentStorageProperties = documentStorageProperties;
        this.googleCloudStorageProperties = googleCloudStorageProperties;
    }

    public GoogleStorageTestResponse upload(MultipartFile file) {
        validate(file);

        String bucketName = requireBucketName();
        String contentType = file.getContentType();
        String objectName = objectName(contentType);
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName))
                .setContentType(contentType)
                .build();

        try {
            Storage storage = createStorageClient(googleCloudStorageProperties);
            storage.create(blobInfo, file.getBytes());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read file", exception);
        } catch (StorageException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not upload file to Google Cloud Storage", exception);
        }

        return new GoogleStorageTestResponse(
                bucketName,
                objectName,
                file.getOriginalFilename(),
                contentType,
                file.getSize()
        );
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
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not load Google Cloud credentials from configured path",
                    exception
            );
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        if (file.getSize() > documentStorageProperties.getMaxSizeBytes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be 10MB or smaller");
        }

        if (!documentStorageProperties.getAllowedContentTypes().contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be a PDF, JPG, or PNG");
        }
    }

    private String objectName(String contentType) {
        return prefix() + "/google-test/" + UUID.randomUUID() + extensionFor(contentType);
    }

    private String prefix() {
        String prefix = googleCloudStorageProperties.getObjectPrefix();
        if (!hasText(prefix)) {
            return "documents";
        }

        return prefix.trim().replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String requireBucketName() {
        if (!hasText(googleCloudStorageProperties.getBucketName())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Google Cloud Storage bucket is not configured");
        }

        return googleCloudStorageProperties.getBucketName().trim();
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> "";
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
