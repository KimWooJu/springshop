package com.springshop.service.payment;

import com.springshop.domain.payment.Payment;
import com.springshop.domain.payment.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 결제 처리 핵심 서비스.
 *
 * <p>결제 요청 / 완료 / 실패 / 취소의 전체 라이프사이클을 담당한다. PG 통신은
 * {@link PaymentGatewayAdapter} 추상 계층을 통해 수행하며, 실제 결제 트랜잭션은
 * 주문 처리 트랜잭션과 분리되어야 하므로 {@code REQUIRES_NEW} 전파 정책을 적용한다.</p>
 *
 * <p>중복 결제 방지를 위해 결제 요청 시점에 Redis 캐시 또는 락 키를 활용하며,
 * 결제 완료 시 도메인 이벤트({@code PaymentCompletedEvent}, {@code PaymentFailedEvent})를
 * 발행한다. 환불 처리는 {@link RefundService} 에 위임한다.</p>
 *
 * @author SpringShop Service Team
 */
public interface PaymentService {

    /**
     * 결제 요청. 주문에 대한 결제 객체를 생성하고 PG 승인 API 를 호출한다.
     */
    Payment requestPayment(PaymentRequest request);

    /**
     * PG 승인 완료 처리. PG 거래번호를 받아 결제를 완료 상태로 전이한다.
     */
    Payment completePayment(Long paymentId, String pgTransactionId, PaymentMethod method);

    /**
     * PG 승인 실패 처리.
     */
    Payment failPayment(Long paymentId, String reason);

    /**
     * 결제 취소 (사용자 요청 또는 시스템 타임아웃).
     */
    Payment cancelPayment(Long paymentId, String reason);

    /**
     * 결제 ID 로 조회.
     */
    Optional<Payment> findById(Long paymentId);

    /**
     * 요청 ID(idempotent key) 로 조회.
     */
    Optional<Payment> findByRequestId(String requestId);

    /**
     * 주문에 대한 가장 최근 결제 조회.
     */
    Optional<Payment> findLatestByOrderId(Long orderId);

    /**
     * 사용자의 결제 이력.
     */
    List<Payment> findUserPayments(Long userId, int limit);

    /**
     * 만료된 보류 상태 결제 일괄 처리 (스케줄러용).
     */
    int expirePendingPayments();

    /**
     * 결제 요청 파라미터.
     */
    record PaymentRequest(Long orderId,
                          Long userId,
                          BigDecimal amount,
                          String currency,
                          PaymentMethod method,
                          String idempotencyKey) {
        public PaymentRequest {
            if (orderId == null) throw new IllegalArgumentException("orderId 필수");
            if (userId == null) throw new IllegalArgumentException("userId 필수");
            if (amount == null || amount.signum() <= 0) {
                throw new IllegalArgumentException("amount는 양수여야 합니다");
            }
            if (currency == null || currency.isBlank()) currency = "KRW";
            if (method == null) throw new IllegalArgumentException("결제 수단 필수");
        }
    }
}
