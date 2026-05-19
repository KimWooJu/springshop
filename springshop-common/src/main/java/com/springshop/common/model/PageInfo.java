package com.springshop.common.model;

/**
 * 페이지네이션 메타 정보.
 *
 * <p>Spring Data {@link org.springframework.data.domain.Page}를 API 응답 형태로
 * 직렬화할 때 사용한다. 클라이언트가 UI 페이지 탐색을 구현할 수 있도록 필요한 모든 정보를 담는다.</p>
 *
 * @param totalElements 전체 항목 수
 * @param totalPages    전체 페이지 수
 * @param currentPage   현재 페이지 번호 (0-based)
 * @param pageSize      페이지 당 항목 수
 * @param isFirst       첫 페이지 여부
 * @param isLast        마지막 페이지 여부
 * @param hasPrevious   이전 페이지 존재 여부
 * @param hasNext       다음 페이지 존재 여부
 */
public record PageInfo(
    long totalElements,
    int totalPages,
    int currentPage,
    int pageSize,
    boolean isFirst,
    boolean isLast,
    boolean hasPrevious,
    boolean hasNext
) {

    /**
     * 기본 필드만으로 {@code PageInfo}를 생성한다.
     * 나머지 boolean 값은 자동 계산된다.
     */
    public static PageInfo of(long totalElements, int totalPages, int currentPage, int pageSize) {
        boolean isFirst = currentPage == 0;
        boolean isLast = totalPages == 0 || currentPage == totalPages - 1;
        boolean hasPrevious = currentPage > 0;
        boolean hasNext = currentPage < totalPages - 1;
        return new PageInfo(
            totalElements, totalPages, currentPage, pageSize,
            isFirst, isLast, hasPrevious, hasNext
        );
    }

    /**
     * Spring Data {@code Page}로부터 {@code PageInfo}를 생성한다.
     */
    public static <T> PageInfo from(org.springframework.data.domain.Page<T> page) {
        return of(
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize()
        );
    }

    /**
     * 빈 페이지 정보를 반환한다.
     */
    public static PageInfo empty(int pageSize) {
        return new PageInfo(0L, 0, 0, pageSize, true, true, false, false);
    }

    /**
     * 다음 페이지 번호. 없으면 현재 페이지를 반환.
     */
    public int nextPage() {
        return hasNext ? currentPage + 1 : currentPage;
    }

    /**
     * 이전 페이지 번호. 없으면 0 반환.
     */
    public int previousPage() {
        return hasPrevious ? currentPage - 1 : 0;
    }
}
