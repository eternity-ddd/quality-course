package org.eternity.food.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

/**
 * 페이징 응답 wrapper. delivery-ddd-style와 동일한 구조.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        Long totalElements,
        Integer totalPages,
        boolean hasNext
) {
    public static <T> PagedResponse<T> from(Slice<T> slice, Pageable pageable) {
        return new PagedResponse<>(
                slice.getContent(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                null,
                null,
                slice.hasNext()
        );
    }

    public static <T> PagedResponse<T> from(Page<T> page, Pageable pageable) {
        return new PagedResponse<>(
                page.getContent(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
