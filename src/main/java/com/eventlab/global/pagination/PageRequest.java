package com.eventlab.global.pagination;

import com.eventlab.global.exception.BusinessException;
import com.eventlab.global.exception.ErrorCode;

public record PageRequest(
        int page,
        int size
) {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    public PageRequest {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_PAGINATION);
        }
    }

    public static PageRequest of(Integer page, Integer size) {
        return new PageRequest(
                page == null ? DEFAULT_PAGE : page,
                size == null ? DEFAULT_SIZE : size
        );
    }

    public long getOffset() {
        return (long) page * size;
    }
}
