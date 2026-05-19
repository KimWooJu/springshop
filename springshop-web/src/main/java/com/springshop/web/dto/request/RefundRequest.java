package com.springshop.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 환불 요청 DTO.
 *
 * <p>전체 환불 / 부분 환불 / 특정 아이템 환불을 표현한다.
 * 환불 사유, 환불 계좌(가상계좌 결제 건), 환불 방식을 받는다.
 *
 * <p>compact constructor 에서 환불 금액과 아이템 ID 목록의 정합성을 검증한다.
 */
@Schema(description = "환불 요청")
public record RefundRequest(

        @Schema(description = "주문 ID", example = "9001")
        @NotNull(message = "주문 ID는 필수입니다.")
        @Positive
        Long orderId,

        @Schema(description = "환불 금액 (전체 환불 시 null 가능)", example = "29500")
        @DecimalMin(value = "0.0", message = "환불 금액은 0 이상이어야 합니다.")
        BigDecimal amount,

        @Schema(description = "환불 유형", allowableValues = {"FULL", "PARTIAL", "ITEM"})
        @NotBlank(message = "환불 유형은 필수입니다.")
        String refundType,

        @Schema(description = "환불 사유 (자유 텍스트)")
        @NotBlank(message = "환불 사유는 필수입니다.")
        @Size(min = 5, max = 500, message = "환불 사유는 5~500자여야 합니다.")
        String reason,

        @Schema(description = "환불 사유 코드", allowableValues = {
                "CHANGE_OF_MIND", "DEFECTIVE_PRODUCT", "WRONG_ITEM", "DELIVERY_DELAY",
                "BETTER_PRICE_FOUND", "QUALITY_ISSUE", "NOT_AS_DESCRIBED", "OTHER"
        })
        String reasonCode,

        @Schema(description = "아이템 환불일 때 대상 OrderItem ID 목록")
        List<Long> orderItemIds,

        @Schema(description = "환불 계좌 정보 (가상계좌/무통장 입금 건만)")
        RefundAccountInfo refundAccountInfo,

        @Schema(description = "관리자 대신 처리 여부 (관리자 토큰일 때)", defaultValue = "false")
        Boolean processedByAdmin,

        @Schema(description = "고객 메모")
        @Size(max = 500)
        String customerMemo,

        @Schema(description = "환불 증빙 이미지 URL 목록")
        List<String> evidenceImageUrls
) {

    public RefundRequest {
        if (refundType == null) {
            throw new IllegalArgumentException("환불 유형은 필수입니다.");
        }
        if ("ITEM".equals(refundType) && (orderItemIds == null || orderItemIds.isEmpty())) {
            throw new IllegalArgumentException("아이템 환불에는 대상 ID가 필요합니다.");
        }
        if ("PARTIAL".equals(refundType) && (amount == null || amount.signum() <= 0)) {
            throw new IllegalArgumentException("부분 환불에는 환불 금액이 필요합니다.");
        }
        orderItemIds = orderItemIds == null ? List.of() : List.copyOf(orderItemIds);
        evidenceImageUrls = evidenceImageUrls == null ? List.of() : List.copyOf(evidenceImageUrls);
        if (processedByAdmin == null) processedByAdmin = Boolean.FALSE;
    }

    public boolean isFullRefund() {
        return "FULL".equalsIgnoreCase(refundType);
    }

    public boolean isPartialRefund() {
        return "PARTIAL".equalsIgnoreCase(refundType);
    }

    public boolean isItemRefund() {
        return "ITEM".equalsIgnoreCase(refundType);
    }

    /**
     * 환불 계좌 정보 (가상 계좌 결제 건 환불 시 필수).
     */
    @Schema(description = "환불 계좌 정보")
    public record RefundAccountInfo(
            @Schema(description = "은행 코드") @NotBlank String bankCode,
            @Schema(description = "계좌 번호") @NotBlank String accountNumber,
            @Schema(description = "예금주") @NotBlank String accountHolder
    ) {}
}
