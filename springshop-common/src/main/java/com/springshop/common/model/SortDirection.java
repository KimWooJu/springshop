package com.springshop.common.model;

/**
 * 정렬 방향 열거형.
 *
 * <p>Spring Data의 {@link org.springframework.data.domain.Sort.Direction}와 매핑되며,
 * 클라이언트로부터 받은 문자열을 안전하게 파싱하는 팩토리 메서드를 제공한다.</p>
 */
public enum SortDirection {

    /** 오름차순. */
    ASC("오름차순"),
    /** 내림차순. */
    DESC("내림차순");

    private final String displayName;

    SortDirection(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 사용자에게 표시할 한국어 이름을 반환한다.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 오름차순 여부를 반환한다.
     */
    public boolean isAscending() {
        return this == ASC;
    }

    /**
     * 내림차순 여부를 반환한다.
     */
    public boolean isDescending() {
        return this == DESC;
    }

    /**
     * 반대 방향을 반환한다.
     */
    public SortDirection reverse() {
        return this == ASC ? DESC : ASC;
    }

    /**
     * 문자열을 {@code SortDirection}으로 변환한다.
     *
     * @param value {@code "ASC"}, {@code "ASCENDING"}, {@code "A"},
     *              {@code "DESC"}, {@code "DESCENDING"}, {@code "D"} 중 하나
     * @return 변환된 {@code SortDirection}. {@code null} 입력은 {@link #ASC} 반환.
     * @throws IllegalArgumentException 유효하지 않은 값
     */
    public static SortDirection fromString(String value) {
        if (value == null) {
            return ASC;
        }
        return switch (value.toUpperCase().trim()) {
            case "ASC", "ASCENDING", "A" -> ASC;
            case "DESC", "DESCENDING", "D" -> DESC;
            default -> throw new IllegalArgumentException("유효하지 않은 정렬 방향: " + value);
        };
    }

    /**
     * Spring Data {@code Sort.Direction}으로 변환한다.
     */
    public org.springframework.data.domain.Sort.Direction toSpringDirection() {
        return this == ASC
            ? org.springframework.data.domain.Sort.Direction.ASC
            : org.springframework.data.domain.Sort.Direction.DESC;
    }

    /**
     * Spring Data {@code Sort.Direction}으로부터 변환한다.
     */
    public static SortDirection from(org.springframework.data.domain.Sort.Direction direction) {
        return direction == org.springframework.data.domain.Sort.Direction.ASC ? ASC : DESC;
    }
}
