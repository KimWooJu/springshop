package com.springshop.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 상품 등록 요청 DTO.
 *
 * <p>관리자/판매자가 새 상품을 등록할 때 사용된다.
 * 옵션 변종(VariantRequest), 이미지(ImageRequest), 태그를 중첩 record 로 받는다.
 */
@Schema(description = "상품 등록 요청")
public record CreateProductRequest(
        @Schema(description = "상품명", required = true)
        @NotBlank(message = "상품명은 필수입니다.")
        @Size(min = 2, max = 200)
        String name,

        @Schema(description = "URL 슬러그 (자동 생성 가능)")
        @Size(max = 200)
        String slug,

        @Schema(description = "짧은 설명 (목록용)")
        @Size(max = 500)
        String shortDescription,

        @Schema(description = "상품 설명 (HTML 허용)", required = true)
        @NotBlank
        @Size(min = 10, max = 50000)
        String description,

        @Schema(description = "정가", required = true)
        @NotNull
        @DecimalMin(value = "0", inclusive = false, message = "가격은 0보다 커야 합니다.")
        @DecimalMax(value = "10000000", message = "가격은 천만원 이하여야 합니다.")
        BigDecimal price,

        @Schema(description = "판매가 (할인 적용가)")
        @DecimalMin(value = "0", inclusive = false)
        BigDecimal salePrice,

        @Schema(description = "원가 (마진 계산용)")
        @DecimalMin(value = "0", inclusive = false)
        BigDecimal costPrice,

        @Schema(description = "통화 코드", example = "KRW")
        String currency,

        @Schema(description = "카테고리 ID", required = true)
        @NotNull(message = "카테고리는 필수입니다.")
        Long categoryId,

        @Schema(description = "브랜드 ID", required = true)
        @NotNull(message = "브랜드는 필수입니다.")
        Long brandId,

        @Schema(description = "상품 상태",
                allowableValues = {"DRAFT", "ACTIVE", "OUT_OF_STOCK", "DISCONTINUED"})
        String status,

        @Schema(description = "옵션 변종 목록")
        @Valid
        List<VariantRequest> variants,

        @Schema(description = "이미지 목록")
        @Valid
        List<ImageRequest> images,

        @Schema(description = "태그 (최대 20개)")
        @Size(max = 20)
        List<@Size(min = 1, max = 30) String> tags,

        @Schema(description = "SEO 메타 제목")
        @Size(max = 200)
        String metaTitle,

        @Schema(description = "SEO 메타 설명")
        @Size(max = 500)
        String metaDescription,

        @Schema(description = "배송비 정책",
                allowableValues = {"FREE", "FIXED", "CONDITIONAL"})
        String shippingPolicy,

        @Schema(description = "고정 배송비")
        @DecimalMin("0")
        BigDecimal shippingFee
) {

    public CreateProductRequest {
        if (currency == null) currency = "KRW";
        if (status == null) status = "DRAFT";
        variants = variants == null ? List.of() : List.copyOf(variants);
        images = images == null ? List.of() : List.copyOf(images);
        tags = tags == null ? List.of() : List.copyOf(tags);
        if (salePrice != null && price != null && salePrice.compareTo(price) > 0) {
            throw new IllegalArgumentException("판매가는 정가보다 클 수 없습니다.");
        }
    }

    /** 옵션 변종 등록 요청. */
    @Schema(description = "옵션 변종")
    public record VariantRequest(
            @Schema(description = "옵션 이름", example = "색상")
            @NotBlank @Size(max = 50) String optionName,

            @Schema(description = "옵션 값", example = "블랙")
            @NotBlank @Size(max = 100) String optionValue,

            @Schema(description = "SKU")
            @Size(max = 100) String sku,

            @Schema(description = "옵션 추가 금액")
            @NotNull @DecimalMin("-1000000") @DecimalMax("1000000")
            BigDecimal additionalPrice,

            @Schema(description = "초기 재고 수량")
            @Min(0) @Max(1_000_000) int stockQuantity,

            @Schema(description = "변종 상태",
                    allowableValues = {"ACTIVE", "INACTIVE"})
            String status
    ) {}

    /** 이미지 등록 요청. */
    @Schema(description = "상품 이미지")
    public record ImageRequest(
            @NotBlank @Size(max = 500) String url,
            @Size(max = 200) String altText,
            @Min(0) @Max(100) int displayOrder,
            boolean isMain
    ) {}

    /** 옵션 없는 단품 여부. */
    public boolean isSingleProduct() {
        return variants.isEmpty();
    }
}
