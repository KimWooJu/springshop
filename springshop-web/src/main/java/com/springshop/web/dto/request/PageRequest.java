package com.springshop.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;

/**
 * 공통 페이지네이션/정렬 요청 DTO.
 *
 * <p>모든 목록 API에서 재사용하는 통합 페이지 요청. Spring Data 의
 * {@link Pageable} 로 변환 가능하다.
 *
 * <p>정렬은 "field,DIR" 형식 — e.g. "createdAt,DESC". 여러 정렬을 콤마로 분리하여 전달한다.
 *
 * <pre>
 *   GET /api/v1/products?page=0&size=20&sort=createdAt,DESC&sort=name,ASC
 * </pre>
 */
@Schema(description = "공통 페이지/정렬 요청")
public record PageRequest(

        @Schema(description = "페이지 번호 (0-base)", example = "0", defaultValue = "0")
        @Min(value = 0, message = "page는 0 이상이어야 합니다.")
        Integer page,

        @Schema(description = "페이지 크기", example = "20", defaultValue = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 200, message = "size는 200 이하여야 합니다.")
        Integer size,

        @Schema(description = "정렬 기준 필드", example = "createdAt")
        String sortBy,

        @Schema(description = "정렬 방향", allowableValues = {"ASC", "DESC"}, defaultValue = "DESC")
        String sortDir,

        @Schema(description = "복합 정렬 표현식 (필드,방향;필드,방향)", example = "createdAt,DESC;name,ASC")
        String sort,

        @Schema(description = "전체 카운트 포함 여부", defaultValue = "true")
        Boolean withTotalCount
) {

    /** 기본 페이지 크기. */
    public static final int DEFAULT_SIZE = 20;
    /** 최대 페이지 크기. */
    public static final int MAX_SIZE = 200;

    public PageRequest {
        if (page == null || page < 0) page = 0;
        if (size == null || size < 1) size = DEFAULT_SIZE;
        if (size > MAX_SIZE) size = MAX_SIZE;
        if (sortBy == null || sortBy.isBlank()) sortBy = "createdAt";
        if (sortDir == null || sortDir.isBlank()) sortDir = "DESC";
        if (!sortDir.equalsIgnoreCase("ASC") && !sortDir.equalsIgnoreCase("DESC")) {
            throw new IllegalArgumentException("정렬 방향은 ASC 또는 DESC 여야 합니다.");
        }
        if (withTotalCount == null) withTotalCount = Boolean.TRUE;
    }

    /** 기본 요청 (page=0, size=20, createdAt DESC). */
    public static PageRequest of() {
        return new PageRequest(0, DEFAULT_SIZE, "createdAt", "DESC", null, true);
    }

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size, "createdAt", "DESC", null, true);
    }

    /** Spring Data Pageable 로 변환. */
    public Pageable toPageable() {
        Sort sortObj = buildSort();
        return org.springframework.data.domain.PageRequest.of(page, size, sortObj);
    }

    /** 복합 정렬 표현식 또는 단일 sortBy를 Sort 로 변환. */
    private Sort buildSort() {
        if (sort != null && !sort.isBlank()) {
            List<Sort.Order> orders = Arrays.stream(sort.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(this::parseOrder)
                    .toList();
            return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
        }
        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        return Sort.by(direction, sortBy);
    }

    private Sort.Order parseOrder(String expr) {
        String[] parts = expr.split(",");
        String field = parts[0].trim();
        Sort.Direction dir = parts.length >= 2
                ? Sort.Direction.fromString(parts[1].trim())
                : Sort.Direction.DESC;
        return new Sort.Order(dir, field);
    }

    public int offset() {
        return page * size;
    }

    public boolean isFirstPage() {
        return page == 0;
    }
}
