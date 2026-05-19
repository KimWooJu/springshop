package com.springshop.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 상품 수정 요청 DTO.
 *
 * <p>부분 업데이트 지원. 옵션/이미지 수정은 별도 엔드포인트
 * (PUT /products/{id}/variants/{variantId}, POST /products/{id}/images 등) 사용.
 */
@Schema(description = "상품 수정 요청")
public record UpdateProductRequest(
        @Schema(description = "상품명")
        @Size(min = 2, max = 200)
        String name,

        @Schema(description = "슬러그")
        @Size(max = 200)
        String slug,

        @Schema(description = "짧은 설명")
        @Size(max = 500)
        String shortDescription,

        @Schema(description = "상품 설명")
        @Size(min = 10, max = 50000)
        String description,

        @Schema(description = "정가")
        @DecimalMin(value = "0", inclusive = false)
        @DecimalMax("10000000")
        BigDecimal price,

        @Schema(description = "판매가")
        @DecimalMin(value = "0", inclusive = false)
        BigDecimal salePrice,

        @Schema(description = "원가")
        @DecimalMin(value = "0", inclusive = false)
        BigDecimal costPrice,

        @Schema(description = "카테고리 ID")
        Long categoryId,

        @Schema(description = "브랜드 ID")
        Long brandId,

        @Schema(description = "상품 상태",
                allowableValues = {"DRAFT", "ACTIVE", "OUT_OF_STOCK", "DISCONTINUED"})
        String status,

        @Schema(description = "태그 (전체 교체)")
        @Size(max = 20)
        List<@Size(min = 1, max = 30) String> tags,

        @Schema(description = "SEO 메타 제목")
        @Size(max = 200)
        String metaTitle,

        @Schema(description = "SEO 메타 설명")
        @Size(max = 500)
        String metaDescription,

        @Schema(description = "배송비 정책")
        String shippingPolicy,

        @Schema(description = "고정 배송비")
        @DecimalMin("0")
        BigDecimal shippingFee,

        @Schema(description = "변경 사유 (감사 로그 기록)")
        @Size(max = 500)
        String changeReason
) {

    public UpdateProductRequest {
        if (name != null) name = name.trim();
        if (salePrice != null && price != null && salePrice.compareTo(price) > 0) {
            throw new IllegalArgumentException("판매가는 정가보다 클 수 없습니다.");
        }
    }

    /** 가격 변경 여부 (히스토리 기록용). */
    public boolean priceChanged() {
        return price != null || salePrice != null;
    }

    /** 변경 필드 카운트. */
    public int changedFieldCount() {
        int count = 0;
        if (name != null) count++;
        if (slug != null) count++;
        if (shortDescription != null) count++;
        if (description != null) count++;
        if (price != null) count++;
        if (salePrice != null) count++;
        if (costPrice != null) count++;
        if (categoryId != null) count++;
        if (brandId != null) count++;
        if (status != null) count++;
        if (tags != null) count++;
        if (metaTitle != null) count++;
        if (metaDescription != null) count++;
        if (shippingPolicy != null) count++;
        if (shippingFee != null) count++;
        return count;
    }
}
