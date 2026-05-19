package com.springshop.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품 상세 응답.
 *
 * <p>상세 페이지에 표시되는 모든 정보 - 기본 정보, 옵션, 이미지, 카테고리/브랜드,
 * 평점/리뷰 수, 재고, 태그, 판매 통계를 포함한다.
 */
@Schema(description = "상품 상세 응답")
public record ProductResponse(
        @Schema(description = "상품 ID", example = "10001") Long id,
        @Schema(description = "상품명") String name,
        @Schema(description = "슬러그(URL용)") String slug,
        @Schema(description = "상품 설명(HTML 허용)") String description,
        @Schema(description = "단가") BigDecimal price,
        @Schema(description = "할인 적용 단가") BigDecimal salePrice,
        @Schema(description = "할인율(%)") int discountPercent,
        @Schema(description = "통화 코드", example = "KRW") String currency,
        @Schema(description = "재고 상태") String stockStatus,
        @Schema(description = "전체 재고 수량") int totalStock,
        @Schema(description = "상품 상태", allowableValues = {"DRAFT", "ACTIVE", "OUT_OF_STOCK", "DISCONTINUED"})
        String status,
        @Schema(description = "카테고리 요약") CategoryRef category,
        @Schema(description = "브랜드 요약") BrandRef brand,
        @Schema(description = "이미지 목록") List<ImageRef> images,
        @Schema(description = "옵션(변종) 목록") List<VariantRef> variants,
        @Schema(description = "태그") List<String> tags,
        @Schema(description = "평균 평점", example = "4.7") double averageRating,
        @Schema(description = "리뷰 개수", example = "1283") long reviewCount,
        @Schema(description = "조회수") long viewCount,
        @Schema(description = "누적 판매 수량") long soldCount,
        @Schema(description = "등록일")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,
        @Schema(description = "수정일")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt
) {

    public ProductResponse {
        images = images == null ? List.of() : List.copyOf(images);
        variants = variants == null ? List.of() : List.copyOf(variants);
        tags = tags == null ? List.of() : List.copyOf(tags);
        currency = currency == null ? "KRW" : currency;
    }

    @Schema(description = "카테고리 요약 참조")
    public record CategoryRef(Long id, String name, String slug) {}

    @Schema(description = "브랜드 요약 참조")
    public record BrandRef(Long id, String name, String logoUrl) {}

    @Schema(description = "상품 이미지 참조")
    public record ImageRef(
            Long id, String url, String altText, int displayOrder, boolean isMain
    ) {}

    @Schema(description = "상품 옵션(변종) 참조")
    public record VariantRef(
            Long id,
            String optionName,
            String optionValue,
            String sku,
            BigDecimal additionalPrice,
            int stockQuantity,
            String status
    ) {
        public BigDecimal effectivePrice(BigDecimal basePrice) {
            return basePrice.add(additionalPrice == null ? BigDecimal.ZERO : additionalPrice);
        }
    }

    /** 할인 적용 가격을 계산한다 (이미 salePrice가 있으면 그대로). */
    public BigDecimal effectivePrice() {
        return salePrice != null ? salePrice : price;
    }

    /** 메인 이미지 URL 반환 (없으면 첫 번째 이미지, 그것도 없으면 null). */
    public String mainImageUrl() {
        return images.stream()
                .filter(ImageRef::isMain)
                .findFirst()
                .map(ImageRef::url)
                .orElseGet(() -> images.isEmpty() ? null : images.get(0).url());
    }
}
