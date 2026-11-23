package com.porter.ksa.controller;

import com.porter.ksa.dto.GoogleStorageTestResponse;
import com.porter.ksa.service.GoogleStorageTestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/google-storage-test")
public class GoogleStorageTestController {

    private final GoogleStorageTestService googleStorageTestService;

    public GoogleStorageTestController(GoogleStorageTestService googleStorageTestService) {
        this.googleStorageTestService = googleStorageTestService;
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public GoogleStorageTestResponse upload(@RequestParam("file") MultipartFile file) {
        return googleStorageTestService.upload(file);
    }
}
