package com.springshop.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 결제 응답.
 *
 * <p>결제 요청 결과, 결제 상세 조회 등에 사용된다.
 * PG 트랜잭션 ID, 카드 정보(마스킹), 할부 개월 등 결제 정보의 모든 세부사항을 담는다.
 */
@Schema(description = "결제 응답")
public record PaymentResponse(
        @Schema(description = "결제 ID") Long id,
        @Schema(description = "주문 ID") Long orderId,
        @Schema(description = "결제 수단",
                allowableValues = {"CREDIT_CARD", "BANK_TRANSFER", "VIRTUAL_ACCOUNT", "KAKAOPAY", "NAVERPAY", "POINT"})
        String method,
        @Schema(description = "결제 금액") BigDecimal amount,
        @Schema(description = "통화", example = "KRW") String currency,
        @Schema(description = "결제 상태",
                allowableValues = {"READY", "IN_PROGRESS", "PAID", "FAILED", "CANCELLED", "REFUNDED", "PARTIAL_REFUNDED"})
        String status,
        @Schema(description = "PG 사", example = "TOSS_PAYMENTS") String pgProvider,
        @Schema(description = "PG 트랜잭션 ID") String transactionId,
        @Schema(description = "승인 번호") String approvalNumber,
        @Schema(description = "카드사", example = "삼성카드") String cardCompany,
        @Schema(description = "카드 번호 (마스킹)", example = "1234-****-****-5678") String cardNumberMasked,
        @Schema(description = "할부 개월수", example = "0") int installmentMonths,
        @Schema(description = "가상계좌 - 은행명") String virtualAccountBank,
        @Schema(description = "가상계좌 - 계좌번호") String virtualAccountNumber,
        @Schema(description = "가상계좌 - 만료일")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime virtualAccountExpiry,
        @Schema(description = "환불 가능 잔액") BigDecimal refundableAmount,
        @Schema(description = "환불 이력 (List)") List<RefundHistoryItem> refundHistory,
        @Schema(description = "결제 요청 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime requestedAt,
        @Schema(description = "결제 완료 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime paidAt,
        @Schema(description = "실패 사유") String failureReason
) {

    public PaymentResponse {
        refundHistory = refundHistory == null ? List.of() : List.copyOf(refundHistory);
        currency = currency == null ? "KRW" : currency;
    }

    @Schema(description = "환불 이력 항목")
    public record RefundHistoryItem(
            Long refundId, BigDecimal amount, String reason, String status,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime refundedAt
    ) {}

    /** 결제가 완료된 상태인지 */
    public boolean isPaid() {
        return "PAID".equals(status);
    }

    /** 환불 가능한지 판단. */
    public boolean isRefundable() {
        return ("PAID".equals(status) || "PARTIAL_REFUNDED".equals(status))
                && refundableAmount != null && refundableAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /** READY/IN_PROGRESS 상태에서 결제 진행 페이지로 리다이렉트할 URL. */
    public String checkoutRedirectUrl() {
        if (!"READY".equals(status) && !"IN_PROGRESS".equals(status)) {
            return null;
        }
        return "/checkout/redirect?pg=" + pgProvider + "&tx=" + transactionId;
    }
}
