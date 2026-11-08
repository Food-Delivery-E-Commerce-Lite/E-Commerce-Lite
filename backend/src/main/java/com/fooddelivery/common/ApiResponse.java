package com.fooddelivery.common;

import java.util.List;
import org.springframework.data.domain.Page;

public record ApiResponse<T>(T data, PageMeta page) {
    public record PageMeta(int page, int size, long totalElements, int totalPages) {}

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, null);
    }

    public static <T> ApiResponse<List<T>> page(Page<T> p) {
        return new ApiResponse<>(p.getContent(),
            new PageMeta(p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages()));
    }
}
