package com.springshop.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 상품 검색/필터 요청 DTO.
 *
 * <p>키워드 + 카테고리/브랜드 필터 + 가격 범위 + 정렬 + 페이지네이션을 한 번에 받는다.
 * 검색 결과는 ProductSummaryResponse 의 PageResponse 로 반환된다.
 */
@Schema(description = "상품 검색 요청")
public record ProductSearchRequest(
        @Schema(description = "검색 키워드 (상품명/설명/태그)")
        @Size(max = 100)
        String keyword,

        @Schema(description = "카테고리 ID (하위 포함)")
        Long categoryId,

        @Schema(description = "브랜드 ID (다중 가능)")
        List<Long> brandIds,

        @Schema(description = "태그 (AND 매칭)")
        @Size(max = 10)
        List<@Size(min = 1, max = 30) String> tags,

        @Schema(description = "최소 가격")
        @DecimalMin("0")
        BigDecimal minPrice,

        @Schema(description = "최대 가격")
        @DecimalMin("0")
        BigDecimal maxPrice,

        @Schema(description = "최소 평점", example = "4")
        @Min(0) @Max(5)
        Integer minRating,

        @Schema(description = "재고 있는 상품만", example = "true")
        Boolean inStockOnly,

        @Schema(description = "할인 상품만", example = "false")
        Boolean onSaleOnly,

        @Schema(description = "신상품만 (30일 이내)", example = "false")
        Boolean newOnly,

        @Schema(description = "정렬 기준",
                allowableValues = {"relevance", "price", "rating", "soldCount", "createdAt", "name"})
        @Pattern(regexp = "^(relevance|price|rating|soldCount|createdAt|name)$")
        String sortBy,

        @Schema(description = "정렬 방향", allowableValues = {"ASC", "DESC"})
        @Pattern(regexp = "^(ASC|DESC)$")
        String sortDir,

        @Schema(description = "페이지 (0-base)", example = "0")
        @Min(0)
        Integer page,

        @Schema(description = "페이지 크기 (1~100)", example = "20")
        @Min(1) @Max(100)
        Integer size
) {

    public ProductSearchRequest {
        if (keyword != null) keyword = keyword.trim();
        if (sortBy == null) sortBy = "relevance";
        if (sortDir == null) sortDir = "DESC";
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (brandIds != null) brandIds = List.copyOf(brandIds);
        if (tags != null) tags = List.copyOf(tags);
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("최소 가격이 최대 가격보다 클 수 없습니다.");
        }
    }

    /** keyword 가 의미 있는 검색어인지 (공백/null 제외). */
    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank() && keyword.length() >= 2;
    }

    /** 가격 필터 활성 여부. */
    public boolean hasPriceFilter() {
        return minPrice != null || maxPrice != null;
    }

    /** 사용자가 옵션 필터 중 하나라도 설정했는지. */
    public boolean hasAnyFilter() {
        return categoryId != null || (brandIds != null && !brandIds.isEmpty())
                || (tags != null && !tags.isEmpty()) || hasPriceFilter()
                || minRating != null || Boolean.TRUE.equals(inStockOnly)
                || Boolean.TRUE.equals(onSaleOnly) || Boolean.TRUE.equals(newOnly);
    }
}
