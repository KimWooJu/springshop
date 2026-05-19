package com.springshop.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 장바구니 상품 추가 요청 DTO.
 *
 * <p>상품 ID와 (선택적으로) 옵션 변형(variant) ID, 수량을 받는다.
 * compact constructor 에서 수량 유효성, 정규화(null → 1)를 수행한다.
 *
 * <p>옵션이 없는 상품은 {@code variantId}를 {@code null}로 전달한다.
 *
 * <pre>
 *   POST /api/v1/cart/items
 *   {
 *     "productId": 1024,
 *     "variantId": 7,
 *     "quantity": 2
 *   }
 * </pre>
 */
@Schema(description = "장바구니 상품 추가 요청")
public record AddToCartRequest(

        @Schema(description = "상품 ID", example = "1024")
        @NotNull(message = "상품 ID는 필수입니다.")
        @Positive(message = "상품 ID는 양수여야 합니다.")
        Long productId,

        @Schema(description = "옵션(변형) ID (선택)", example = "7", nullable = true)
        @Positive(message = "옵션 ID는 양수여야 합니다.")
        Long variantId,

        @Schema(description = "추가 수량", example = "1", defaultValue = "1")
        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        @Max(value = 999, message = "수량은 999 이하여야 합니다.")
        Integer quantity,

        @Schema(description = "쿠폰 미리 적용 코드 (선택)", nullable = true)
        String preApplyCouponCode,

        @Schema(description = "장바구니 ID (비회원/세션 분리 시 사용)", nullable = true)
        String cartSessionId
) {

    public AddToCartRequest {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("상품 ID가 올바르지 않습니다.");
        }
        if (preApplyCouponCode != null) {
            preApplyCouponCode = preApplyCouponCode.trim().toUpperCase();
            if (preApplyCouponCode.isBlank()) {
                preApplyCouponCode = null;
            }
        }
    }

    /** 옵션 변형이 지정되어 있는지 여부. */
    public boolean hasVariant() {
        return variantId != null && variantId > 0;
    }

    /** 쿠폰 사전 적용 여부. */
    public boolean hasPreAppliedCoupon() {
        return preApplyCouponCode != null && !preApplyCouponCode.isBlank();
    }

    /** 비회원/게스트 장바구니 여부. */
    public boolean isGuestCart() {
        return cartSessionId != null && !cartSessionId.isBlank();
    }
}
