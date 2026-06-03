package org.eternity.food.base.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

/**
 * 페이징 결과를 *항목 리스트 + 메타데이터*로 묶는 공용 응답 wrapper.
 *
 * <p>{@code totalElements}/{@code totalPages}는 {@link Slice}로부터 만들면 {@code null}.
 * {@link Page}로부터 만들면 값이 있음.
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
