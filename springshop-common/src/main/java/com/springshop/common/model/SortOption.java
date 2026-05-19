package com.springshop.common.model;

import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;

/**
 * 단일 정렬 옵션.
 *
 * <p>{@code field:direction} 형식의 문자열을 파싱하여 Spring Data {@link Sort} 객체로
 * 변환하는 책임을 진다. 여러 정렬 기준을 한 번에 받기 위한 {@link #parseMultiple(String)}을
 * 제공한다.</p>
 *
 * @param field     정렬할 필드명
 * @param direction 정렬 방향 (null이면 ASC)
 */
public record SortOption(String field, SortDirection direction) {

    /**
     * 컴팩트 생성자로 필드 유효성을 검증한다.
     */
    public SortOption {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("정렬 필드가 비어있습니다");
        }
        if (direction == null) {
            direction = SortDirection.ASC;
        }
    }

    /**
     * 오름차순 정렬 옵션을 생성한다.
     */
    public static SortOption of(String field) {
        return new SortOption(field, SortDirection.ASC);
    }

    /**
     * 오름차순 정렬 옵션을 생성한다.
     */
    public static SortOption asc(String field) {
        return new SortOption(field, SortDirection.ASC);
    }

    /**
     * 내림차순 정렬 옵션을 생성한다.
     */
    public static SortOption desc(String field) {
        return new SortOption(field, SortDirection.DESC);
    }

    /**
     * Spring Data {@code Sort}로 변환한다.
     */
    public Sort toSort() {
        return direction.isAscending()
            ? Sort.by(field).ascending()
            : Sort.by(field).descending();
    }

    /**
     * 쉼표로 구분된 여러 정렬 기준을 파싱한다.
     *
     * <p>예: {@code "name:asc,createdAt:desc"} → 두 개의 {@code SortOption}.</p>
     *
     * @param sortParam {@code field[:direction]} 형식. null/빈 문자열이면
     *                  기본값 {@code [createdAt:desc]} 반환.
     */
    public static List<SortOption> parseMultiple(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return List.of(SortOption.desc("createdAt"));
        }
        return Arrays.stream(sortParam.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .map(s -> {
                String[] parts = s.split(":");
                String field = parts[0].trim();
                SortDirection dir = parts.length > 1
                    ? SortDirection.fromString(parts[1])
                    : SortDirection.ASC;
                return new SortOption(field, dir);
            })
            .toList();
    }

    /**
     * {@code SortOption} 리스트를 Spring Data {@code Sort}로 변환한다.
     */
    public static Sort toSort(List<SortOption> options) {
        if (options == null || options.isEmpty()) {
            return Sort.by("createdAt").descending();
        }
        return Sort.by(options.stream()
            .map(opt -> opt.direction().isAscending()
                ? Sort.Order.asc(opt.field())
                : Sort.Order.desc(opt.field()))
            .toList());
    }

    /**
     * 문자열 표현. ({@code field:ASC}/{@code field:DESC} 형태)
     */
    public String toQueryParam() {
        return field + ":" + direction.name();
    }
}
