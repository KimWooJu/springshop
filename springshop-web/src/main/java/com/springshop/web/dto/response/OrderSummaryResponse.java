package com.springshop.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 목록/카드용 요약 응답.
 *
 * <p>주문 목록 페이지, 마이페이지 최근 주문, 관리자 주문 표 등에 사용된다.
 * 첫 상품의 썸네일과 "외 N건" 형태로 표시하기 위한 minimal 필드만 포함한다.
 */
@Schema(description = "주문 요약 응답")
public record OrderSummaryResponse(
        @Schema(description = "주문 ID") Long id,
        @Schema(description = "주문 번호", example = "ORD-20260519-00012") String orderNumber,
        @Schema(description = "주문 상태") String status,
        @Schema(description = "상태 한국어 표시", example = "결제완료") String statusDisplay,
        @Schema(description = "첫 상품명", example = "맥북 프로 16인치") String firstProductName,
        @Schema(description = "첫 상품 썸네일") String firstProductThumbnail,
        @Schema(description = "총 상품 종류 수", example = "3") int itemCount,
        @Schema(description = "총 수량", example = "5") int totalQuantity,
        @Schema(description = "최종 결제 금액") BigDecimal finalAmount,
        @Schema(description = "통화", example = "KRW") String currency,
        @Schema(description = "결제 수단", example = "CREDIT_CARD") String paymentMethod,
        @Schema(description = "배송 추적 가능 여부") boolean trackable,
        @Schema(description = "리뷰 작성 가능 여부") boolean reviewable,
        @Schema(description = "취소 가능 여부") boolean cancellable,
        @Schema(description = "주문 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt
) {

    /** 표시용 "맥북 프로 16인치 외 2건" 텍스트. */
    public String displayTitle() {
        if (itemCount <= 1) {
            return firstProductName == null ? "주문 항목 없음" : firstProductName;
        }
        return firstProductName + " 외 " + (itemCount - 1) + "건";
    }

    /** PENDING/PAID 등 영문 상태 → 한국어 변환 헬퍼. */
    public static String toStatusDisplay(String status) {
        return switch (status) {
            case "PENDING" -> "결제대기";
            case "PAID" -> "결제완료";
            case "PREPARING" -> "상품준비중";
            case "SHIPPED" -> "배송중";
            case "DELIVERED" -> "배송완료";
            case "CANCELLED" -> "취소";
            case "RETURNED" -> "반품";
            case null -> "알수없음";
            default -> status;
        };
    }

    /** 단순 팩토리. */
    public static OrderSummaryResponse of(Long id, String orderNumber, String status,
                                          String firstName, String firstThumbnail,
                                          int itemCount, BigDecimal finalAmount,
                                          LocalDateTime createdAt) {
        return new OrderSummaryResponse(
                id, orderNumber, status, toStatusDisplay(status),
                firstName, firstThumbnail, itemCount, itemCount, finalAmount, "KRW",
                "CREDIT_CARD", "SHIPPED".equals(status), "DELIVERED".equals(status),
                !"DELIVERED".equals(status) && !"CANCELLED".equals(status),
                createdAt
        );
    }
}
