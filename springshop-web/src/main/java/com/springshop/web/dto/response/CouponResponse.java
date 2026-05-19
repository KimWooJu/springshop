package com.springshop.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 쿠폰 응답.
 *
 * <p>사용자 보유 쿠폰 목록, 관리자 쿠폰 관리에서 사용된다.
 * 정액/정률 할인, 최소 주문 금액, 최대 할인 한도 등을 모두 포함한다.
 */
@Schema(description = "쿠폰 응답")
public record CouponResponse(
        @Schema(description = "쿠폰 ID") Long id,
        @Schema(description = "쿠폰 코드", example = "WELCOME2026") String code,
        @Schema(description = "쿠폰 이름", example = "신규 회원 환영 10% 할인") String name,
        @Schema(description = "쿠폰 설명") String description,
        @Schema(description = "할인 유형",
                allowableValues = {"FIXED_AMOUNT", "PERCENTAGE", "FREE_SHIPPING"})
        String discountType,
        @Schema(description = "할인 값 (정액=원, 정률=%)") BigDecimal discountValue,
        @Schema(description = "최소 주문 금액") BigDecimal minOrderAmount,
        @Schema(description = "최대 할인 금액 (정률 쿠폰의 cap)") BigDecimal maxDiscountAmount,
        @Schema(description = "총 발급 수량 (관리자 한정)") Integer totalQuantity,
        @Schema(description = "사용 수량") int usedCount,
        @Schema(description = "남은 수량") Integer remainingQuantity,
        @Schema(description = "사용자별 사용 가능 횟수", example = "1") int usableTimesPerUser,
        @Schema(description = "내 사용 횟수", example = "0") int myUsedCount,
        @Schema(description = "적용 카테고리 ID (전체 적용은 null)") Long applicableCategoryId,
        @Schema(description = "적용 상품 ID (전체 적용은 null)") Long applicableProductId,
        @Schema(description = "사용 가능 시작일")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate startDate,
        @Schema(description = "사용 가능 종료일")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate endDate,
        @Schema(description = "활성 여부") boolean active,
        @Schema(description = "내가 사용 가능한 상태") boolean usable,
        @Schema(description = "발급일")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime issuedAt
) {

    /** 표시용 할인 문자열 "10% 할인 (최대 5,000원)" */
    public String discountDisplay() {
        return switch (discountType) {
            case "PERCENTAGE" -> {
                String base = discountValue + "% 할인";
                yield maxDiscountAmount != null
                        ? base + " (최대 " + formatAmount(maxDiscountAmount) + "원)"
                        : base;
            }
            case "FIXED_AMOUNT" -> formatAmount(discountValue) + "원 할인";
            case "FREE_SHIPPING" -> "무료 배송";
            case null -> "";
            default -> discountType;
        };
    }

    /** 만료 여부 */
    public boolean isExpired() {
        return endDate != null && endDate.isBefore(LocalDate.now());
    }

    /** 사용 가능 일자 사이인지 */
    public boolean isWithinPeriod() {
        LocalDate today = LocalDate.now();
        return (startDate == null || !startDate.isAfter(today))
                && (endDate == null || !endDate.isBefore(today));
    }

    /** 사용량 비율(%) - 관리자 화면용. */
    public double usageRate() {
        if (totalQuantity == null || totalQuantity == 0) return 0.0;
        return Math.round(usedCount * 1000.0 / totalQuantity) / 10.0;
    }

    private static String formatAmount(BigDecimal amount) {
        return amount.toBigInteger().toString().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ",");
    }
}
