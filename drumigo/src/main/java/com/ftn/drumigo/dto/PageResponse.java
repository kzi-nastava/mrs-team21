package com.ftn.drumigo.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Simple DTO for paginated responses. Ensures consistent JSON shape for frontend.
 */
public record PageResponse<T>(
    List<T> content,
    long totalElements,
    int totalPages,
    int size,
    int number
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getSize(),
            page.getNumber()
        );
    }
}
