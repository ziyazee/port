package com.porter.ksa.service;

import com.porter.ksa.dto.DocumentRecordResponse;
import com.porter.ksa.entity.DocumentOwnerType;
import com.porter.ksa.entity.DocumentRecord;
import com.porter.ksa.entity.DocumentReference;
import com.porter.ksa.repository.CustomerAccountRepository;
import com.porter.ksa.repository.DocumentRecordRepository;
import com.porter.ksa.repository.TransportOrderRepository;
import com.porter.ksa.storage.DocumentStorage;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class DocumentService {

    private final DocumentRecordRepository documents;
    private final CustomerAccountRepository customerAccounts;
    private final TransportOrderRepository transportOrders;
    private final DocumentStorage documentStorage;

    public DocumentService(
            DocumentRecordRepository documents,
            CustomerAccountRepository customerAccounts,
            TransportOrderRepository transportOrders,
            DocumentStorage documentStorage
    ) {
        this.documents = documents;
        this.customerAccounts = customerAccounts;
        this.transportOrders = transportOrders;
        this.documentStorage = documentStorage;
    }

    public DocumentRecordResponse upload(
            DocumentOwnerType ownerType,
            Long ownerId,
            String documentType,
            String uploadedBy,
            MultipartFile file
    ) {
        validateOwner(ownerType, ownerId);
        String normalizedDocumentType = requireText(documentType, "Document type is required");
        String normalizedUploadedBy = requireText(uploadedBy, "Uploaded by is required");

        DocumentReference reference = documentStorage.store(ownerType, ownerId, normalizedDocumentType, file);
        DocumentRecord document = new DocumentRecord(
                ownerType,
                ownerId,
                normalizedDocumentType,
                reference.getStorageKey(),
                requireText(reference.getOriginalFilename(), "Document filename is required"),
                normalizedUploadedBy
        );

        return toResponse(documents.save(document));
    }

    @Transactional(readOnly = true)
    public List<DocumentRecordResponse> findByOwner(DocumentOwnerType ownerType, Long ownerId) {
        validateOwner(ownerType, ownerId);
        return documents.findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(ownerType, ownerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void delete(Long id) {
        DocumentRecord document = documents.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        documents.delete(document);
    }

    private void validateOwner(DocumentOwnerType ownerType, Long ownerId) {
        if (ownerType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document owner type is required");
        }

        if (ownerId == null || ownerId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document owner id is required");
        }

        switch (ownerType) {
            case CUSTOMER_ACCOUNT -> {
                if (!customerAccounts.existsById(ownerId)) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer account not found");
                }
            }
            case ORDER -> {
                if (!transportOrders.existsById(ownerId)) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
                }
            }
            case REPORT -> {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Report document ownership is not available until reports are implemented"
                );
            }
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        return value.trim();
    }

    private DocumentRecordResponse toResponse(DocumentRecord document) {
        return new DocumentRecordResponse(
                document.getId(),
                document.getOwnerType(),
                document.getOwnerId(),
                document.getDocumentType(),
                document.getStorageKey(),
                document.getOriginalFilename(),
                document.getUploadedBy(),
                document.getCreatedAt()
        );
    }
}
