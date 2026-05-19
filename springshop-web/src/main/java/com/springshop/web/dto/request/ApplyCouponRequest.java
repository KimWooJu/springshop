package com.springshop.web.dto.request;

import com.springshop.web.validator.CouponCodeConstraint;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 쿠폰 적용 요청 DTO.
 *
 * <p>주문 화면 또는 장바구니 화면에서 쿠폰을 적용할 때 사용한다.
 * 쿠폰 코드, 적용 대상 금액, 적용 대상 식별자(주문/장바구니)를 받는다.
 *
 * <p>compact constructor 에서 쿠폰 코드 대문자화, 공백 제거, 음수 금액 검증을 수행한다.
 */
@Schema(description = "쿠폰 적용 요청")
public record ApplyCouponRequest(

        @Schema(description = "쿠폰 코드", example = "WELCOME10")
        @NotBlank(message = "쿠폰 코드는 필수입니다.")
        @CouponCodeConstraint
        String couponCode,

        @Schema(description = "주문 예정 금액 (할인 전)", example = "59000")
        @NotNull(message = "주문 금액은 필수입니다.")
        @DecimalMin(value = "0.0", inclusive = false, message = "주문 금액은 0보다 커야 합니다.")
        BigDecimal orderAmount,

        @Schema(description = "적용 대상 주문 ID (주문 화면에서 사용)", nullable = true)
        Long orderId,

        @Schema(description = "적용 대상 장바구니 ID (장바구니에서 사용)", nullable = true)
        Long cartId,

        @Schema(description = "추천인 사용자 ID (특정 쿠폰의 검증용)", nullable = true)
        Long referrerUserId,

        @Schema(description = "검증만 수행 (true 시 적용은 하지 않음)", defaultValue = "false")
        Boolean validateOnly
) {

    public ApplyCouponRequest {
        if (couponCode != null) {
            couponCode = couponCode.trim().toUpperCase();
        }
        if (orderAmount == null || orderAmount.signum() <= 0) {
            throw new IllegalArgumentException("주문 금액은 0보다 커야 합니다.");
        }
        if (orderId != null && cartId != null) {
            throw new IllegalArgumentException("주문 ID와 장바구니 ID는 동시에 지정할 수 없습니다.");
        }
        if (validateOnly == null) {
            validateOnly = Boolean.FALSE;
        }
    }

    /** 장바구니 화면 적용 여부. */
    public boolean isCartContext() {
        return cartId != null;
    }

    /** 주문 화면 적용 여부. */
    public boolean isOrderContext() {
        return orderId != null;
    }
}
