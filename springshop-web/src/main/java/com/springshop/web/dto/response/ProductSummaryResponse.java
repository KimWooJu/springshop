package com.springshop.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품 목록/카드 요약 응답.
 *
 * <p>검색 결과, 카테고리/브랜드 목록, 추천 위젯 등에 사용된다.
 * 응답 크기를 줄이기 위해 ProductResponse 보다 적은 필드만 포함한다.
 */
@Schema(description = "상품 목록/카드용 요약 응답")
public record ProductSummaryResponse(
        @Schema(description = "상품 ID", example = "10001") Long id,
        @Schema(description = "상품명") String name,
        @Schema(description = "슬러그") String slug,
        @Schema(description = "정가") BigDecimal price,
        @Schema(description = "판매가") BigDecimal salePrice,
        @Schema(description = "할인율(%)", example = "15") int discountPercent,
        @Schema(description = "썸네일 URL") String thumbnailUrl,
        @Schema(description = "브랜드명") String brandName,
        @Schema(description = "카테고리명") String categoryName,
        @Schema(description = "평균 평점", example = "4.5") double averageRating,
        @Schema(description = "리뷰 수") long reviewCount,
        @Schema(description = "누적 판매") long soldCount,
        @Schema(description = "할인 중 여부", example = "true") boolean isOnSale,
        @Schema(description = "신상품 뱃지", example = "false") boolean isNew,
        @Schema(description = "베스트 뱃지", example = "true") boolean isBest,
        @Schema(description = "재고 여부", example = "true") boolean inStock,
        @Schema(description = "주요 태그(최대 5개)") List<String> tags,
        @Schema(description = "등록일")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt
) {

    public ProductSummaryResponse {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    /** 검색/목록 API에서 빠르게 사용할 수 있는 기본 팩토리. */
    public static ProductSummaryResponse of(Long id, String name, BigDecimal price,
                                            String thumbnailUrl, double rating, long sold) {
        return new ProductSummaryResponse(
                id, name, null, price, price, 0, thumbnailUrl, null, null,
                rating, 0L, sold, false, false, false, true, List.of(), null
        );
    }

    /** 할인 적용된 인스턴스를 만든다. */
    public ProductSummaryResponse withSalePrice(BigDecimal newSale) {
        int discount = price.compareTo(BigDecimal.ZERO) > 0 ?
                price.subtract(newSale).multiply(BigDecimal.valueOf(100))
                        .divide(price, java.math.RoundingMode.DOWN).intValue() : 0;
        return new ProductSummaryResponse(
                id, name, slug, price, newSale, discount, thumbnailUrl, brandName, categoryName,
                averageRating, reviewCount, soldCount, true, isNew, isBest, inStock, tags, createdAt
        );
    }

    /** 실 판매가 (할인 적용 후) */
    public BigDecimal effectivePrice() {
        return salePrice != null ? salePrice : price;
    }

    /** 표시용 별점 문자열 ★★★★☆ */
    public String ratingStars() {
        int filled = (int) Math.round(averageRating);
        return "★".repeat(filled) + "☆".repeat(5 - filled);
    }
}
