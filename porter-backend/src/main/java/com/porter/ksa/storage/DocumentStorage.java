package com.porter.ksa.storage;

import com.porter.ksa.entity.DocumentOwnerType;
import com.porter.ksa.entity.DocumentReference;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentStorage {
    DocumentReference store(Long customerAccountId, DocumentType documentType, MultipartFile file);

    DocumentReference store(DocumentOwnerType ownerType, Long ownerId, String documentType, MultipartFile file);
}
