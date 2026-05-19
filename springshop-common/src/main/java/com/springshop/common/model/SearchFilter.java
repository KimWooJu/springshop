package com.springshop.common.model;

import java.time.LocalDate;
import java.util.List;

/**
 * 검색 필터 추상화 (sealed interface).
 *
 * <p>도메인별 검색 화면에서 사용하는 다양한 필터 유형을 타입 안전하게 표현한다.
 * Java 21의 sealed interface + record patterns + pattern matching switch를
 * 활용하여 모든 case를 컴파일러가 검증한다.</p>
 *
 * <p>구현체:</p>
 * <ul>
 *   <li>{@link TextFilter} - 텍스트 일치/LIKE 검색</li>
 *   <li>{@link RangeFilter} - 숫자/문자열 범위</li>
 *   <li>{@link MultiSelectFilter} - 다중 선택 IN 검색</li>
 *   <li>{@link BooleanFilter} - 불리언 플래그</li>
 *   <li>{@link DateRangeFilter} - 날짜 구간</li>
 * </ul>
 */
public sealed interface SearchFilter
    permits SearchFilter.TextFilter,
            SearchFilter.RangeFilter,
            SearchFilter.MultiSelectFilter,
            SearchFilter.BooleanFilter,
            SearchFilter.DateRangeFilter {

    /** 필터가 적용되는 필드명. */
    String field();

    /**
     * 텍스트 검색 필터.
     *
     * @param field      필드명
     * @param value      검색 값
     * @param exactMatch 정확 일치 여부 (false면 LIKE 검색)
     */
    record TextFilter(String field, String value, boolean exactMatch) implements SearchFilter {
        public TextFilter(String field, String value) {
            this(field, value, false);
        }
        /** LIKE 절에 들어갈 패턴 (양쪽 와일드카드). */
        public String getLikeValue() {
            return exactMatch ? value : "%" + value + "%";
        }
    }

    /**
     * 범위 검색 필터. min/max 중 하나만 지정해도 무방하다.
     */
    record RangeFilter(String field, Object min, Object max) implements SearchFilter {
        public boolean hasMin() { return min != null; }
        public boolean hasMax() { return max != null; }
        public boolean isBetween() { return hasMin() && hasMax(); }
    }

    /**
     * 다중 선택 IN 검색 필터.
     */
    record MultiSelectFilter(String field, List<Object> values) implements SearchFilter {
        public MultiSelectFilter {
            values = List.copyOf(values);
        }
        public boolean isEmpty() { return values.isEmpty(); }
        public int size() { return values.size(); }
    }

    /**
     * 불리언 플래그 필터.
     */
    record BooleanFilter(String field, boolean value) implements SearchFilter {}

    /**
     * 날짜 범위 필터.
     */
    record DateRangeFilter(String field, LocalDate from, LocalDate to) implements SearchFilter {
        public DateRangeFilter {
            if (from != null && to != null && from.isAfter(to)) {
                throw new IllegalArgumentException("시작일이 종료일보다 클 수 없습니다");
            }
        }
        public boolean hasFrom() { return from != null; }
        public boolean hasTo() { return to != null; }
    }

    /**
     * 사람이 읽을 수 있는 표현으로 변환한다.
     * (디버깅/로깅 용도)
     */
    default String toDescription() {
        return switch (this) {
            case TextFilter(var f, var v, var exact) ->
                f + " " + (exact ? "=" : "LIKE") + " " + v;
            case RangeFilter(var f, var min, var max) ->
                f + " BETWEEN " + min + " AND " + max;
            case MultiSelectFilter(var f, var vals) ->
                f + " IN " + vals;
            case BooleanFilter(var f, var v) ->
                f + " = " + v;
            case DateRangeFilter(var f, var from, var to) ->
                f + " BETWEEN " + from + " AND " + to;
        };
    }
}
