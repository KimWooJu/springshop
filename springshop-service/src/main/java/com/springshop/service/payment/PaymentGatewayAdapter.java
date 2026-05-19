package com.springshop.service.payment;

import com.springshop.domain.payment.Payment;
import com.springshop.domain.payment.PaymentMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * 결제 게이트웨이 어댑터.
 *
 * <p>PG 사(토스/카카오/네이버 등)에 대한 추상 인터페이스를 제공한다. 각 PG 별 구현은
 * 내부 {@code sealed interface PgProvider} 의 permits 멤버로 표현되며, 실제 호출은
 * 정적 메서드를 통해 라우팅된다.</p>
 *
 * <p>본 클래스는 데모/샘플용으로 실제 PG 통신을 수행하지 않고, 결정적 결과(성공/실패)를
 * 환경 설정 값에 따라 생성한다. 테스트 환경에서는 항상 성공을 반환하도록 기본값이 설정된다.</p>
 */
@Component
public class PaymentGatewayAdapter {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayAdapter.class);

    /**
     * PG 사 sealed 인터페이스.
     */
    public sealed interface PgProvider permits TossPayments, KakaoPay, NaverPay {
        String code();
        String displayName();
    }

    public record TossPayments(String apiKey, String secretKey) implements PgProvider {
        public TossPayments {
            Objects.requireNonNull(apiKey, "apiKey 필수");
            Objects.requireNonNull(secretKey, "secretKey 필수");
        }
        @Override public String code() { return "TOSS"; }
        @Override public String displayName() { return "토스페이먼츠"; }
    }

    public record KakaoPay(String cid, String adminKey) implements PgProvider {
        public KakaoPay {
            Objects.requireNonNull(cid, "cid 필수");
            Objects.requireNonNull(adminKey, "adminKey 필수");
        }
        @Override public String code() { return "KAKAO"; }
        @Override public String displayName() { return "카카오페이"; }
    }

    public record NaverPay(String merchantId, String clientSecret) implements PgProvider {
        public NaverPay {
            Objects.requireNonNull(merchantId, "merchantId 필수");
            Objects.requireNonNull(clientSecret, "clientSecret 필수");
        }
        @Override public String code() { return "NAVER"; }
        @Override public String displayName() { return "네이버페이"; }
    }

    /**
     * PG 승인 결과.
     */
    public record PaymentApprovalResult(boolean success,
                                        String pgTransactionId,
                                        String errorCode,
                                        String errorMessage) {
        public static PaymentApprovalResult ok(String pgTransactionId) {
            return new PaymentApprovalResult(true, pgTransactionId, null, null);
        }
        public static PaymentApprovalResult fail(String code, String message) {
            return new PaymentApprovalResult(false, null, code, message);
        }
    }

    /**
     * PG 환불 결과.
     */
    public record RefundResult(boolean success,
                               String pgRefundId,
                               String errorMessage) {
        public static RefundResult ok(String pgRefundId) {
            return new RefundResult(true, pgRefundId, null);
        }
        public static RefundResult fail(String message) {
            return new RefundResult(false, null, message);
        }
    }

    private final boolean mockMode;
    private final boolean simulateFailure;

    public PaymentGatewayAdapter(
            @Value("${payment.gateway.mock:true}") boolean mockMode,
            @Value("${payment.gateway.simulate-failure:false}") boolean simulateFailure) {
        this.mockMode = mockMode;
        this.simulateFailure = simulateFailure;
    }

    /**
     * 결제 승인 요청.
     */
    public PaymentApprovalResult approve(Payment payment, PaymentMethod method) {
        Objects.requireNonNull(payment, "payment 필수");
        Objects.requireNonNull(method, "결제 수단 필수");

        // 결제 수단 기반 라우팅 — pattern matching for switch (Java 21+)
        PgProvider provider = routeProvider(method);
        log.info("PG 승인 호출: paymentId={}, provider={}, amount={}",
                payment.getId(), provider.displayName(), payment.getAmount());

        if (mockMode) {
            return mockApprove(payment, provider);
        }

        return switch (provider) {
            case TossPayments toss -> approveViaToss(payment, toss);
            case KakaoPay kakao -> approveViaKakao(payment, kakao);
            case NaverPay naver -> approveViaNaver(payment, naver);
        };
    }

    /**
     * 환불 요청.
     */
    public RefundResult refund(Payment payment, BigDecimal amount) {
        Objects.requireNonNull(payment, "payment 필수");
        Objects.requireNonNull(amount, "amount 필수");

        if (amount.signum() <= 0) {
            return RefundResult.fail("환불 금액은 양수여야 합니다");
        }

        PgProvider provider = routeProvider(payment.getMethodType());
        log.info("PG 환불 호출: paymentId={}, provider={}, amount={}",
                payment.getId(), provider.displayName(), amount);

        if (mockMode) {
            return mockRefund(payment, provider, amount);
        }

        return switch (provider) {
            case TossPayments toss -> refundViaToss(payment, amount, toss);
            case KakaoPay kakao -> refundViaKakao(payment, amount, kakao);
            case NaverPay naver -> refundViaNaver(payment, amount, naver);
        };
    }

    /**
     * 결제 상태 조회 (옵션).
     */
    public PaymentApprovalResult query(String pgTransactionId, PgProvider provider) {
        Objects.requireNonNull(pgTransactionId, "pgTransactionId 필수");
        log.debug("PG 상태 조회: pgTxId={}, provider={}", pgTransactionId, provider.displayName());
        if (mockMode) {
            return PaymentApprovalResult.ok(pgTransactionId);
        }
        // 실제 구현은 PG 별 API 호출
        return PaymentApprovalResult.ok(pgTransactionId);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private PgProvider routeProvider(PaymentMethod method) {
        return switch (method.typeCode()) {
            case "CREDIT_CARD", "BANK_TRANSFER" -> new TossPayments("toss-key", "toss-secret");
            case "MOBILE_PAYMENT" -> new KakaoPay("CID0001", "kakao-admin");
            case "VIRTUAL_ACCOUNT" -> new NaverPay("NV001", "naver-secret");
            default -> throw new IllegalArgumentException("미지원 결제 수단: " + method.typeCode());
        };
    }

    private PgProvider routeProvider(Payment.MethodTypeColumn methodType) {
        return switch (methodType) {
            case CREDIT_CARD, BANK_TRANSFER -> new TossPayments("toss-key", "toss-secret");
            case MOBILE_PAYMENT -> new KakaoPay("CID0001", "kakao-admin");
            case VIRTUAL_ACCOUNT -> new NaverPay("NV001", "naver-secret");
        };
    }

    private PaymentApprovalResult mockApprove(Payment payment, PgProvider provider) {
        if (simulateFailure) {
            return PaymentApprovalResult.fail("PG_MOCK_FAIL", "Mock 강제 실패");
        }
        String pgTxId = provider.code() + "-" + UUID.randomUUID().toString().substring(0, 16);
        log.debug("Mock PG 승인 성공: paymentId={}, pgTxId={}", payment.getId(), pgTxId);
        return PaymentApprovalResult.ok(pgTxId);
    }

    private RefundResult mockRefund(Payment payment, PgProvider provider, BigDecimal amount) {
        if (simulateFailure) {
            return RefundResult.fail("Mock 강제 실패");
        }
        String refundId = provider.code() + "-RF-" + UUID.randomUUID().toString().substring(0, 12);
        log.debug("Mock PG 환불 성공: paymentId={}, amount={}, refundId={}",
                payment.getId(), amount, refundId);
        return RefundResult.ok(refundId);
    }

    private PaymentApprovalResult approveViaToss(Payment payment, TossPayments toss) {
        // 실제 HTTP 호출은 WebClient/RestClient 로 구현. 본 예제에서는 mock 응답.
        log.debug("토스 승인 호출 (apiKey 길이={})", toss.apiKey().length());
        return PaymentApprovalResult.ok("TOSS-" + payment.getId());
    }

    private PaymentApprovalResult approveViaKakao(Payment payment, KakaoPay kakao) {
        log.debug("카카오 승인 호출 (cid={})", kakao.cid());
        return PaymentApprovalResult.ok("KAKAO-" + payment.getId());
    }

    private PaymentApprovalResult approveViaNaver(Payment payment, NaverPay naver) {
        log.debug("네이버 승인 호출 (merchantId={})", naver.merchantId());
        return PaymentApprovalResult.ok("NAVER-" + payment.getId());
    }

    private RefundResult refundViaToss(Payment payment, BigDecimal amount, TossPayments toss) {
        log.debug("토스 환불 호출 (apiKey 길이={})", toss.apiKey().length());
        return RefundResult.ok("TOSS-RF-" + payment.getId());
    }

    private RefundResult refundViaKakao(Payment payment, BigDecimal amount, KakaoPay kakao) {
        log.debug("카카오 환불 호출 (cid={})", kakao.cid());
        return RefundResult.ok("KAKAO-RF-" + payment.getId());
    }

    private RefundResult refundViaNaver(Payment payment, BigDecimal amount, NaverPay naver) {
        log.debug("네이버 환불 호출 (merchantId={})", naver.merchantId());
        return RefundResult.ok("NAVER-RF-" + payment.getId());
    }
}
