package com.porter.ksa.controller;

import com.porter.ksa.dto.DocumentRecordResponse;
import com.porter.ksa.entity.DocumentOwnerType;
import com.porter.ksa.service.DocumentService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(
            path = "/{ownerType}/{ownerId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentRecordResponse upload(
            @PathVariable DocumentOwnerType ownerType,
            @PathVariable Long ownerId,
            @RequestParam String documentType,
            @RequestParam String uploadedBy,
            @RequestParam MultipartFile file
    ) {
        return documentService.upload(ownerType, ownerId, documentType, uploadedBy, file);
    }

    @GetMapping("/{ownerType}/{ownerId}")
    public List<DocumentRecordResponse> findByOwner(
            @PathVariable DocumentOwnerType ownerType,
            @PathVariable Long ownerId
    ) {
        return documentService.findByOwner(ownerType, ownerId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        documentService.delete(id);
    }
}
