package com.springshop.common.util;

import com.springshop.common.constant.AppConstants;
import com.springshop.common.model.PageInfo;
import com.springshop.common.model.SortDirection;
import com.springshop.common.model.SortOption;

import java.util.List;
import java.util.function.Function;

/**
 * 페이지네이션 헬퍼.
 * Spring Data PageRequest 의존성 없이 자체적으로 동작한다.
 */
public final class PageUtils {

    private PageUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static int validatePageSize(int size) {
        return validatePageSize(size, AppConstants.MAX_PAGE_SIZE);
    }

    public static int validatePageSize(int size, int maxSize) {
        if (size <= 0) return AppConstants.DEFAULT_PAGE_SIZE;
        return Math.min(size, maxSize);
    }

    public static int validatePage(int page) {
        return Math.max(0, page);
    }

    public static int calculateOffset(int page, int size) {
        return validatePage(page) * validatePageSize(size);
    }

    public static int calculateTotalPages(long totalElements, int pageSize) {
        if (totalElements <= 0 || pageSize <= 0) return 0;
        return (int) Math.ceil((double) totalElements / pageSize);
    }

    public static PageInfo toPageInfo(int page, int size, long totalElements) {
        int totalPages = calculateTotalPages(totalElements, size);
        return new PageInfo(
                totalElements,
                totalPages,
                page,
                size,
                page == 0,
                page >= totalPages - 1,
                page > 0,
                page < totalPages - 1
        );
    }

    /**
     * 페이지된 결과의 항목을 변환하여 새 PageResponse 생성.
     */
    public static <S, T> PageResponse<T> map(PageResponse<S> source, Function<S, T> mapper) {
        List<T> mapped = source.content().stream().map(mapper).toList();
        return new PageResponse<>(mapped, source.pageInfo());
    }

    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(List.of(), toPageInfo(page, size, 0));
    }

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long total) {
        return new PageResponse<>(content, toPageInfo(page, size, total));
    }

    /**
     * 정렬 옵션 문자열 파싱. "name,asc;price,desc" 형식.
     */
    public static List<SortOption> parseSort(String sortStr) {
        if (sortStr == null || sortStr.isBlank()) return List.of();
        return SortOption.parseMultiple(sortStr);
    }

    public record PageResponse<T>(List<T> content, PageInfo pageInfo) {
        public int size() {
            return content == null ? 0 : content.size();
        }

        public boolean isEmpty() {
            return content == null || content.isEmpty();
        }
    }

    public static SortDirection parseDirection(String direction) {
        return SortDirection.fromString(direction);
    }
}
