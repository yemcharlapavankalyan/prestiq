package org.prestiq.recommendation.dto.response;

import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean isLast
) {
    public static <T> PagedResponse<T> of(List<T> content, int pageNumber, int pageSize, long totalElements) {
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;
        boolean isLast = (pageNumber + 1) >= totalPages;
        return new PagedResponse<>(content, pageNumber, pageSize, totalElements, totalPages, isLast);
    }
}
