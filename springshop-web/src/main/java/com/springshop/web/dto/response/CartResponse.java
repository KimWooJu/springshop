package com.springshop.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 장바구니 응답.
 *
 * <p>장바구니 페이지 표시에 필요한 모든 정보 - 항목 목록, 합계, 할인, 배송비,
 * 적용된 쿠폰을 포함한다. 비회원 게스트 장바구니도 동일한 구조를 사용한다.
 */
@Schema(description = "장바구니 응답")
public record CartResponse(
        @Schema(description = "장바구니 ID") Long id,
        @Schema(description = "사용자 ID (게스트는 null)") Long userId,
        @Schema(description = "세션 키 (게스트용)") String sessionKey,
        @Schema(description = "장바구니 항목") List<CartItemRef> items,
        @Schema(description = "상품 합계") BigDecimal subtotal,
        @Schema(description = "상품 할인 합계") BigDecimal discountAmount,
        @Schema(description = "쿠폰 할인") BigDecimal couponDiscount,
        @Schema(description = "배송비") BigDecimal shippingFee,
        @Schema(description = "최종 결제 예상 금액") BigDecimal finalAmount,
        @Schema(description = "적립 예상 포인트") int estimatedPoints,
        @Schema(description = "적용 쿠폰 정보") CouponInfo couponApplied,
        @Schema(description = "총 항목 수") int totalItemCount,
        @Schema(description = "총 수량") int totalQuantity,
        @Schema(description = "최소 주문 금액 충족 여부") boolean minOrderMet,
        @Schema(description = "무료 배송 충족 여부") boolean freeShippingMet,
        @Schema(description = "무료 배송까지 필요 금액") BigDecimal amountToFreeShipping,
        @Schema(description = "최근 갱신")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt
) {

    public CartResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    @Schema(description = "장바구니 항목")
    public record CartItemRef(
            Long id,
            Long productId,
            String productName,
            String thumbnailUrl,
            Long variantId,
            String optionDisplay,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal salePrice,
            BigDecimal lineTotal,
            int stockAvailable,
            boolean outOfStock,
            boolean selected,
            String status
    ) {

        public BigDecimal effectivePrice() {
            return salePrice != null ? salePrice : unitPrice;
        }

        public BigDecimal recalculate() {
            return effectivePrice().multiply(BigDecimal.valueOf(quantity));
        }

        public boolean overStock() {
            return quantity > stockAvailable;
        }
    }

    @Schema(description = "쿠폰 적용 정보")
    public record CouponInfo(
            Long couponId, String code, String name, String discountType,
            BigDecimal discountValue, BigDecimal appliedAmount
    ) {}

    /** 빈 장바구니 헬퍼. */
    public static CartResponse empty(Long userId) {
        return new CartResponse(
                null, userId, null, List.of(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                0, null, 0, 0, false, false, BigDecimal.ZERO, LocalDateTime.now()
        );
    }

    /** 전체 상품 수량 합계. */
    public int sumQuantities() {
        return items.stream().mapToInt(CartItemRef::quantity).sum();
    }

    /** 재고 부족 항목 존재 여부. */
    public boolean hasOutOfStockItem() {
        return items.stream().anyMatch(CartItemRef::outOfStock);
    }
}
