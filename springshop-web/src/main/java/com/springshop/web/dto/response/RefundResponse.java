package com.springshop.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 환불 응답.
 *
 * <p>환불 요청 결과, 환불 상세 조회에 사용된다.
 * 환불 방식(원결제수단/계좌이체/포인트)별로 처리 일정이 다르므로
 * processedAt 외에 expectedCompletionAt 도 함께 제공한다.
 */
@Schema(description = "환불 응답")
public record RefundResponse(
        @Schema(description = "환불 ID") Long id,
        @Schema(description = "결제 ID") Long paymentId,
        @Schema(description = "주문 ID") Long orderId,
        @Schema(description = "환불 금액") BigDecimal amount,
        @Schema(description = "통화", example = "KRW") String currency,
        @Schema(description = "환불 사유") String reason,
        @Schema(description = "환불 사유 상세 (관리자 메모)") String adminNote,
        @Schema(description = "환불 방식",
                allowableValues = {"ORIGINAL", "BANK_TRANSFER", "POINT"})
        String refundMethod,
        @Schema(description = "환불 상태",
                allowableValues = {"REQUESTED", "APPROVED", "PROCESSING", "COMPLETED", "REJECTED"})
        String status,
        @Schema(description = "원결제 수단", example = "CREDIT_CARD") String originalMethod,
        @Schema(description = "PG 환불 트랜잭션 ID") String refundTransactionId,
        @Schema(description = "은행 계좌 (BANK_TRANSFER일 때)") String bankAccount,
        @Schema(description = "예금주") String bankAccountHolder,
        @Schema(description = "은행명") String bankName,
        @Schema(description = "부분 환불 여부", example = "false") boolean partial,
        @Schema(description = "요청 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime requestedAt,
        @Schema(description = "처리 완료 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime processedAt,
        @Schema(description = "환불 예상 완료 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime expectedCompletionAt,
        @Schema(description = "거부 사유 (REJECTED 일 때)") String rejectReason
) {

    /** 요청 직후 단순 헬퍼. */
    public static RefundResponse requested(Long id, Long paymentId, Long orderId,
                                           BigDecimal amount, String reason, boolean partial) {
        LocalDateTime now = LocalDateTime.now();
        return new RefundResponse(
                id, paymentId, orderId, amount, "KRW", reason, null,
                "ORIGINAL", "REQUESTED", null, null, null, null, null,
                partial, now, null, now.plusDays(3), null
        );
    }

    /** 환불 완료까지 며칠 남았는지 (음수면 지연). */
    public long daysUntilCompletion() {
        if (expectedCompletionAt == null) return 0L;
        return java.time.Duration.between(LocalDateTime.now(), expectedCompletionAt).toDays();
    }

    /** 완료 여부. */
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    /** 거부 여부. */
    public boolean isRejected() {
        return "REJECTED".equals(status);
    }
}
