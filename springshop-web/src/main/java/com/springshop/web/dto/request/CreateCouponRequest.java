package com.springshop.web.dto.request;

import com.springshop.web.validator.CouponCodeConstraint;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 쿠폰 발급/생성 요청 DTO (관리자용).
 *
 * <p>고정 금액 / 정률 / 무료 배송 등 다양한 쿠폰 정책을 표현한다.
 * compact constructor 에서 할인 정책의 일관성, 사용 기간 검증, 코드 정규화를 수행한다.
 *
 * <pre>
 *   POST /api/v1/admin/coupons
 *   {
 *     "code": "WELCOME10",
 *     "name": "신규 회원 10% 할인",
 *     "discountType": "PERCENT",
 *     "discountValue": 10,
 *     "minimumOrderAmount": 30000,
 *     "maxDiscountAmount": 5000,
 *     "validFrom": "2026-06-01T00:00:00",
 *     "validUntil": "2026-12-31T23:59:59",
 *     "totalIssueQuantity": 1000
 *   }
 * </pre>
 */
@Schema(description = "쿠폰 생성 요청 (관리자)")
public record CreateCouponRequest(

        @Schema(description = "쿠폰 코드 (대문자 영문+숫자, 6-20자)", example = "WELCOME10")
        @NotBlank(message = "쿠폰 코드는 필수입니다.")
        @CouponCodeConstraint
        String code,

        @Schema(description = "쿠폰 이름 (사용자 노출)", example = "신규 회원 10% 할인")
        @NotBlank(message = "쿠폰 이름은 필수입니다.")
        @Size(min = 2, max = 100, message = "쿠폰 이름은 2~100자여야 합니다.")
        String name,

        @Schema(description = "쿠폰 설명")
        @Size(max = 500)
        String description,

        @Schema(description = "할인 유형", allowableValues = {"FIXED", "PERCENT", "FREE_SHIPPING"})
        @NotBlank(message = "할인 유형은 필수입니다.")
        String discountType,

        @Schema(description = "할인 값 (FIXED: 원, PERCENT: %)", example = "10")
        @NotNull(message = "할인 값은 필수입니다.")
        @DecimalMin(value = "0.0", inclusive = false, message = "할인 값은 0보다 커야 합니다.")
        BigDecimal discountValue,

        @Schema(description = "최소 주문 금액", example = "30000")
        @DecimalMin(value = "0.0", message = "최소 주문 금액은 0 이상이어야 합니다.")
        BigDecimal minimumOrderAmount,

        @Schema(description = "최대 할인 금액 (PERCENT 쿠폰의 상한)", example = "5000")
        @DecimalMin(value = "0.0", message = "최대 할인 금액은 0 이상이어야 합니다.")
        BigDecimal maxDiscountAmount,

        @Schema(description = "사용 시작 일시")
        @NotNull(message = "사용 시작 일시는 필수입니다.")
        LocalDateTime validFrom,

        @Schema(description = "사용 종료 일시")
        @NotNull(message = "사용 종료 일시는 필수입니다.")
        @Future(message = "사용 종료 일시는 미래여야 합니다.")
        LocalDateTime validUntil,

        @Schema(description = "총 발행 수량 (null=무제한)")
        @Min(value = 1, message = "발행 수량은 1 이상이어야 합니다.")
        Integer totalIssueQuantity,

        @Schema(description = "사용자당 최대 사용 횟수", defaultValue = "1")
        @Min(value = 1)
        Integer maxUsagePerUser,

        @Schema(description = "적용 가능한 카테고리 ID 목록 (null=전체)")
        List<Long> applicableCategoryIds,

        @Schema(description = "적용 가능한 상품 ID 목록 (null=전체)")
        List<Long> applicableProductIds,

        @Schema(description = "신규 회원 전용 여부", defaultValue = "false")
        Boolean newCustomerOnly,

        @Schema(description = "다른 쿠폰과 중복 사용 가능 여부", defaultValue = "false")
        Boolean stackable
) {

    public CreateCouponRequest {
        if (code != null) {
            code = code.trim().toUpperCase();
        }
        if (validFrom != null && validUntil != null && !validUntil.isAfter(validFrom)) {
            throw new IllegalArgumentException("사용 종료 일시는 시작 일시 이후여야 합니다.");
        }
        if ("PERCENT".equals(discountType) && discountValue != null
                && discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("정률 할인은 100% 이하여야 합니다.");
        }
        if ("FREE_SHIPPING".equals(discountType)) {
            // 무료 배송은 할인 값 무시
            discountValue = BigDecimal.ZERO;
        }
        applicableCategoryIds = applicableCategoryIds == null ? List.of() : List.copyOf(applicableCategoryIds);
        applicableProductIds = applicableProductIds == null ? List.of() : List.copyOf(applicableProductIds);
        if (newCustomerOnly == null) newCustomerOnly = Boolean.FALSE;
        if (stackable == null) stackable = Boolean.FALSE;
        if (maxUsagePerUser == null) maxUsagePerUser = 1;
    }

    public boolean isUnlimited() {
        return totalIssueQuantity == null;
    }

    public boolean hasCategoryRestriction() {
        return !applicableCategoryIds.isEmpty();
    }

    public boolean hasProductRestriction() {
        return !applicableProductIds.isEmpty();
    }
}
