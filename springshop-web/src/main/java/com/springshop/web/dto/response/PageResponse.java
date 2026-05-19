package com.springshop.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 제네릭 페이지 응답 래퍼.
 *
 * <p>Spring Data의 {@link Page}를 그대로 노출하는 대신 JSON 직렬화에 안정적인
 * record 형태로 변환하여 반환한다.
 *
 * @param <T> 페이지 요소 타입
 */
@Schema(description = "페이지 응답 래퍼")
public record PageResponse<T>(
        @Schema(description = "현재 페이지 데이터") List<T> content,
        @Schema(description = "총 요소 수") long totalElements,
        @Schema(description = "총 페이지 수") int totalPages,
        @Schema(description = "현재 페이지(0-base)") int currentPage,
        @Schema(description = "페이지 크기") int pageSize,
        @Schema(description = "현재 페이지 요소 수") int numberOfElements,
        @Schema(description = "첫 페이지 여부") boolean first,
        @Schema(description = "마지막 페이지 여부") boolean last,
        @Schema(description = "빈 페이지 여부") boolean empty,
        @Schema(description = "정렬 키", example = "createdAt") String sortBy,
        @Schema(description = "정렬 방향", example = "DESC") String sortDirection
) {

    public PageResponse {
        content = content == null ? List.of() : List.copyOf(content);
    }

    /** Spring Data Page → PageResponse 변환. */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.getNumberOfElements(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty(),
                null,
                null
        );
    }

    /** Spring Data Page + 정렬 메타 → PageResponse. */
    public static <T> PageResponse<T> from(Page<T> page, String sortBy, String sortDir) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.getNumberOfElements(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty(),
                sortBy,
                sortDir
        );
    }

    /** Spring Data Page를 다른 타입으로 변환하면서 PageResponse로 감싼다. */
    public static <S, T> PageResponse<T> map(Page<S> page, Function<S, T> mapper) {
        List<T> mapped = page.getContent().stream().map(mapper).toList();
        return new PageResponse<>(
                mapped,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                mapped.size(),
                page.isFirst(),
                page.isLast(),
                mapped.isEmpty(),
                null,
                null
        );
    }

    /** 단순 리스트 → 단일 페이지 응답. */
    public static <T> PageResponse<T> single(List<T> items) {
        return new PageResponse<>(
                items, items.size(), 1, 0, items.size(), items.size(),
                true, true, items.isEmpty(), null, null
        );
    }

    /** 빈 페이지 응답. */
    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(
                List.of(), 0L, 0, page, size, 0, true, true, true, null, null
        );
    }
}
