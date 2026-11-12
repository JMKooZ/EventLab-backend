package com.eventlab.global.pagination;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int page,
        int size,
        int totalPages
) {
    public static <T> PageResponse<T> of(List<T> content, long totalElements, PageRequest pageRequest) {
        int totalPages = (int) Math.ceil((double) totalElements / pageRequest.size());

        return new PageResponse<>(
                content,
                totalElements,
                pageRequest.page(),
                pageRequest.size(),
                totalPages
        );
    }
}
