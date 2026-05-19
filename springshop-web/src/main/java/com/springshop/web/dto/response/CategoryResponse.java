package com.springshop.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * 카테고리 트리 응답.
 *
 * <p>재귀적 children 필드를 통해 카테고리 트리 전체를 표현한다.
 * 깊이 제한이 없으므로 클라이언트가 무한 트리를 렌더링할 수 있어야 한다.
 */
@Schema(description = "카테고리 트리 응답")
public record CategoryResponse(
        @Schema(description = "카테고리 ID", example = "301") Long id,
        @Schema(description = "이름", example = "노트북") String name,
        @Schema(description = "URL 슬러그", example = "laptops") String slug,
        @Schema(description = "설명") String description,
        @Schema(description = "아이콘 URL") String iconUrl,
        @Schema(description = "트리 레벨 (루트=0)", example = "2") int level,
        @Schema(description = "정렬 우선순위") int displayOrder,
        @Schema(description = "상위 카테고리 ID") Long parentId,
        @Schema(description = "하위 카테고리 목록 (재귀)") List<CategoryResponse> children,
        @Schema(description = "상품 수", example = "1283") long productCount,
        @Schema(description = "활성화 여부") boolean active
) {

    public CategoryResponse {
        children = children == null ? List.of() : List.copyOf(children);
    }

    /** 자식 없는 단순 노드 헬퍼. */
    public static CategoryResponse leaf(Long id, String name, String slug, int level) {
        return new CategoryResponse(id, name, slug, null, null, level, 0, null,
                List.of(), 0L, true);
    }

    /** 자식 노드를 가진 트리 노드 헬퍼. */
    public static CategoryResponse branch(Long id, String name, String slug, int level,
                                          List<CategoryResponse> children) {
        return new CategoryResponse(id, name, slug, null, null, level, 0, null,
                children, 0L, true);
    }

    /** 자식을 추가한 새로운 인스턴스 반환 (불변성 유지). */
    public CategoryResponse withChildren(List<CategoryResponse> newChildren) {
        return new CategoryResponse(id, name, slug, description, iconUrl, level, displayOrder,
                parentId, newChildren, productCount, active);
    }

    /** 트리를 순회하여 모든 노드를 평탄화한다. */
    public List<CategoryResponse> flatten() {
        List<CategoryResponse> result = new ArrayList<>();
        result.add(this);
        for (CategoryResponse child : children) {
            result.addAll(child.flatten());
        }
        return result;
    }

    /** 리프 노드 여부 */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /** 트리 깊이 측정. */
    public int depth() {
        if (children.isEmpty()) return 1;
        return 1 + children.stream().mapToInt(CategoryResponse::depth).max().orElse(0);
    }
}
