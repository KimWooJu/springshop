package com.springshop.service.payment;

import com.springshop.domain.payment.Refund;

import java.math.BigDecimal;
import java.util.List;

/**
 * 환불 서비스.
 *
 * <p>완료된 결제에 대한 환불 요청을 처리한다. 부분 환불을 지원하며, 환불 가능 금액
 * 검증(전체 결제 금액에서 이미 환불된 금액 제외) 후 PG 환불 API 를 호출한다.
 * 처리 결과에 따라 도메인 이벤트({@code RefundCompletedEvent})를 발행한다.</p>
 *
 * @author SpringShop Service Team
 */
public interface RefundService {

    /**
     * 환불 요청. 부분 환불을 위해 amount 를 명시한다.
     */
    Refund requestRefund(RefundRequest request);

    /**
     * 환불 ID 로 조회.
     */
    Refund getRefund(Long refundId);

    /**
     * 결제 ID 에 대한 모든 환불 이력.
     */
    List<Refund> getRefundsByPaymentId(Long paymentId);

    /**
     * 환불 가능 금액 조회.
     */
    BigDecimal getRefundableAmount(Long paymentId);

    /**
     * 환불 진행 (수동 처리용).
     */
    Refund processRefund(Long refundId);

    /**
     * 환불 실패 처리.
     */
    Refund failRefund(Long refundId, String reason);

    /**
     * 환불 요청 파라미터.
     */
    record RefundRequest(Long paymentId, BigDecimal amount, String reason, String refundMethod) {
        public RefundRequest {
            if (paymentId == null) throw new IllegalArgumentException("paymentId 필수");
            if (amount == null || amount.signum() <= 0) {
                throw new IllegalArgumentException("환불 금액은 양수여야 합니다");
            }
            if (reason == null || reason.isBlank()) reason = "사용자 요청";
            if (refundMethod == null || refundMethod.isBlank()) refundMethod = "ORIGINAL";
        }
    }
}
