package com.springshop.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 카테고리 빵부스러기(breadcrumb) 응답.
 *
 * <p>상품 상세 페이지에서 "전자제품 &gt; 노트북 &gt; 게이밍 노트북" 처럼
 * 부모 경로를 표시하기 위해 사용된다. 첫 요소가 최상위 루트, 마지막이 현재 카테고리이다.
 */
@Schema(description = "카테고리 경로(breadcrumb) 응답")
public record CategoryBreadcrumbResponse(
        @Schema(description = "현재 카테고리 ID") Long currentId,
        @Schema(description = "경로 (root → current)") List<CategoryItem> path,
        @Schema(description = "총 깊이") int depth
) {

    public CategoryBreadcrumbResponse {
        path = path == null ? List.of() : List.copyOf(path);
    }

    /**
     * 빵부스러기의 한 노드.
     */
    @Schema(description = "경로 노드")
    public record CategoryItem(
            @Schema(description = "카테고리 ID") Long id,
            @Schema(description = "이름") String name,
            @Schema(description = "URL 슬러그") String slug,
            @Schema(description = "레벨", example = "1") int level,
            @Schema(description = "현재 노드 여부") boolean current
    ) {
        public static CategoryItem of(Long id, String name, String slug, int level) {
            return new CategoryItem(id, name, slug, level, false);
        }

        public static CategoryItem currentOf(Long id, String name, String slug, int level) {
            return new CategoryItem(id, name, slug, level, true);
        }
    }

    /** 단일 경로 헬퍼. */
    public static CategoryBreadcrumbResponse of(Long currentId, List<CategoryItem> path) {
        return new CategoryBreadcrumbResponse(currentId, path, path.size());
    }

    /** 슬래시로 결합된 표시 문자열. */
    public String displayString() {
        return String.join(" > ", path.stream().map(CategoryItem::name).toList());
    }

    /** 첫 노드(루트)를 반환한다. */
    public CategoryItem root() {
        return path.isEmpty() ? null : path.get(0);
    }

    /** 마지막 노드(현재)를 반환한다. */
    public CategoryItem current() {
        return path.isEmpty() ? null : path.get(path.size() - 1);
    }

    /** 부모(직전) 노드를 반환한다. */
    public CategoryItem parent() {
        return path.size() < 2 ? null : path.get(path.size() - 2);
    }
}
