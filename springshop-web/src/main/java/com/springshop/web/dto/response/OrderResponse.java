package com.springshop.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 상세 응답.
 *
 * <p>주문 한 건의 모든 정보 - 주문 상품들, 배송지, 결제 정보, 적용된 쿠폰,
 * 금액 분해(상품 합계/할인/배송비/최종), 상태 이력을 포함한다.
 */
@Schema(description = "주문 상세 응답")
public record OrderResponse(
        @Schema(description = "주문 ID") Long id,
        @Schema(description = "주문 번호", example = "ORD-20260519-00012") String orderNumber,
        @Schema(description = "주문자 ID") Long userId,
        @Schema(description = "주문 상태",
                allowableValues = {"PENDING", "PAID", "PREPARING", "SHIPPED", "DELIVERED", "CANCELLED", "RETURNED"})
        String status,
        @Schema(description = "주문 상품 목록") List<OrderItemRef> items,
        @Schema(description = "배송 정보") ShippingInfo shipping,
        @Schema(description = "결제 정보") PaymentInfo payment,
        @Schema(description = "금액 분해") AmountBreakdown amounts,
        @Schema(description = "적용 쿠폰") CouponApplied coupon,
        @Schema(description = "주문 메모") String memo,
        @Schema(description = "주문 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,
        @Schema(description = "결제 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime paidAt,
        @Schema(description = "발송 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime shippedAt,
        @Schema(description = "수령 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime deliveredAt
) {

    public OrderResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    @Schema(description = "주문 항목")
    public record OrderItemRef(
            Long id, Long productId, String productName, String thumbnailUrl,
            Long variantId, String optionName, String optionValue,
            int quantity, BigDecimal unitPrice, BigDecimal lineTotal
    ) {
        public BigDecimal calculateLineTotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    @Schema(description = "배송 정보")
    public record ShippingInfo(
            String recipientName, String phone, String zipCode, String street, String detail,
            String carrier, String trackingNumber, String memo,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
            java.time.LocalDate estimatedDelivery
    ) {}

    @Schema(description = "결제 요약 (상세는 PaymentResponse 참고)")
    public record PaymentInfo(
            Long paymentId, String method, String status, BigDecimal amount,
            String transactionId, String pgProvider
    ) {}

    @Schema(description = "금액 분해")
    public record AmountBreakdown(
            BigDecimal subtotal, BigDecimal discountAmount, BigDecimal couponDiscount,
            BigDecimal shippingFee, BigDecimal taxAmount, BigDecimal totalAmount,
            int pointsUsed, int pointsEarned
    ) {
        public BigDecimal totalDiscount() {
            return discountAmount.add(couponDiscount == null ? BigDecimal.ZERO : couponDiscount);
        }
    }

    @Schema(description = "적용된 쿠폰")
    public record CouponApplied(
            Long couponId, String code, String name, String discountType, BigDecimal discountValue
    ) {}

    /** 취소 가능 여부 (배송 전까지만). */
    public boolean isCancellable() {
        return switch (status) {
            case "PENDING", "PAID", "PREPARING" -> true;
            default -> false;
        };
    }

    /** 반품 가능 여부. */
    public boolean isReturnable() {
        return "DELIVERED".equals(status) && deliveredAt != null
                && deliveredAt.isAfter(LocalDateTime.now().minusDays(7));
    }
}
