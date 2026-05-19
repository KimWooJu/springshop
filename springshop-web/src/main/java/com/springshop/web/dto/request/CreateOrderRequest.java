package com.springshop.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 주문 생성 요청 DTO.
 *
 * <p>장바구니 결제 화면에서 주문 확정 시 호출된다.
 * 주문 항목, 배송지, 결제 수단, 쿠폰 코드, 포인트 사용 등을 한 번에 받는다.
 */
@Schema(description = "주문 생성 요청")
public record CreateOrderRequest(
        @Schema(description = "주문 상품 목록", required = true)
        @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다.")
        @Valid
        List<OrderItemRequest> items,

        @Schema(description = "배송지 주소 ID (기존 주소 사용)")
        Long shippingAddressId,

        @Schema(description = "신규 배송지 (shippingAddressId 미사용 시)")
        @Valid
        CreateAddressRequest newShippingAddress,

        @Schema(description = "신규 배송지 저장 여부")
        boolean saveNewAddress,

        @Schema(description = "쿠폰 코드")
        @Size(max = 50)
        String couponCode,

        @Schema(description = "사용 포인트", example = "1000")
        @Min(0) @Max(1_000_000)
        Integer pointsToUse,

        @Schema(description = "결제 수단",
                allowableValues = {"CREDIT_CARD", "BANK_TRANSFER", "VIRTUAL_ACCOUNT",
                        "KAKAOPAY", "NAVERPAY", "TOSS_PAY", "POINT"},
                required = true)
        @NotNull
        @Pattern(regexp = "^(CREDIT_CARD|BANK_TRANSFER|VIRTUAL_ACCOUNT|KAKAOPAY|NAVERPAY|TOSS_PAY|POINT)$")
        String paymentMethod,

        @Schema(description = "할부 개월 수 (신용카드)", example = "0")
        @Min(0) @Max(36)
        Integer installmentMonths,

        @Schema(description = "주문 메모")
        @Size(max = 500)
        String memo,

        @Schema(description = "현금영수증 발급 여부")
        boolean issueReceipt,

        @Schema(description = "현금영수증 - 사업자번호 또는 휴대전화")
        @Size(max = 20)
        String receiptIdentifier,

        @Schema(description = "선물 주문 여부")
        boolean isGift,

        @Schema(description = "선물 메시지")
        @Size(max = 200)
        String giftMessage,

        @Schema(description = "마케팅 출처 (광고 추적용)")
        @Size(max = 50)
        String marketingSource,

        @Schema(description = "약관 동의 (필수)")
        @jakarta.validation.constraints.AssertTrue(message = "주문 약관에 동의해야 합니다.")
        boolean agreeOrderTerms,

        @Schema(description = "개인정보 제3자 제공 동의 (필수)")
        @jakarta.validation.constraints.AssertTrue(message = "개인정보 제공에 동의해야 합니다.")
        boolean agreePrivacyProvision
) {

    public CreateOrderRequest {
        items = items == null ? List.of() : List.copyOf(items);
        if (shippingAddressId == null && newShippingAddress == null) {
            throw new IllegalArgumentException("배송지를 지정해야 합니다.");
        }
    }

    /** 주문 항목. */
    @Schema(description = "주문 항목")
    public record OrderItemRequest(
            @Schema(description = "장바구니 아이템 ID (있으면 우선)") Long cartItemId,
            @Schema(description = "상품 ID", required = true)
            @NotNull Long productId,
            @Schema(description = "옵션 변종 ID") Long variantId,
            @Schema(description = "수량", required = true)
            @NotNull @Min(1) @Max(999) Integer quantity
    ) {}

    /** 총 수량 합계. */
    public int totalQuantity() {
        return items.stream().mapToInt(OrderItemRequest::quantity).sum();
    }
}
