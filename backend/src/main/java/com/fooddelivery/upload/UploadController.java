package com.fooddelivery.upload;

import com.fooddelivery.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {
    private final FileStorageService storage;

    public UploadController(FileStorageService storage) {
        this.storage = storage;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FileStorageService.StoredFile> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(storage.store(file));
    }
}
