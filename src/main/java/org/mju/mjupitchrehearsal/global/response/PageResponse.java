package org.mju.mjupitchrehearsal.global.response;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 목록 응답 형식. 페이지 번호는 1부터 시작합니다.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages, boolean hasNext) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
