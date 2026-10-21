package com.fooddelivery.common.exception;

import org.springframework.http.HttpStatus;

/** Lỗi nghiệp vụ có mã lỗi và HTTP status rõ ràng. */
public class BusinessException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }
}
