package com.fooddelivery.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resource, Object id) {
        super("RESOURCE_NOT_FOUND", resource + " không tồn tại: " + id, HttpStatus.NOT_FOUND);
    }
}
