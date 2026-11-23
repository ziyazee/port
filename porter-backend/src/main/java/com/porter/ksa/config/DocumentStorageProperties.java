package com.porter.ksa.config;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "porter.document-storage.local")
public class DocumentStorageProperties {

    private String basePath = System.getProperty("java.io.tmpdir") + "/porter-documents";
    private long maxSizeBytes = 10 * 1024 * 1024;
    private Set<String> allowedContentTypes = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public Set<String> getAllowedContentTypes() {
        return Set.copyOf(allowedContentTypes);
    }

    public void setAllowedContentTypes(Set<String> allowedContentTypes) {
        this.allowedContentTypes = Set.copyOf(allowedContentTypes);
    }
}
