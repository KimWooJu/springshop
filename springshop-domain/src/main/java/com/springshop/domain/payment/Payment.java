package com.springshop.domain.payment;

import com.springshop.domain.base.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 결제 애그리거트 루트.
 *
 * <p>주문 1건에 대해 결제 시도가 1회 이상 발생할 수 있으며 각 시도는 별도의 Payment 엔티티로
 * 저장된다. 결제 수단({@link PaymentMethod})은 sealed 타입으로 표현되지만 JPA 매핑이
 * 까다로우므로 마스킹된 표현 문자열과 메타데이터는 단순 칼럼으로 저장한다.</p>
 *
 * <p>상태({@link PaymentStatus})는 칼럼에 라벨로 저장되고 복원 시 sealed record 인스턴스로
 * 변환된다. 결제 완료/실패 등 상태 변경마다 도메인 이벤트가 발행되어 후속 처리
 * (주문 확정, 알림, 적립 등)를 트리거한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_request_id", columnNames = "request_id"),
        indexes = {
                @Index(name = "idx_payment_order", columnList = "order_id"),
                @Index(name = "idx_payment_user", columnList = "user_id"),
                @Index(name = "idx_payment_status", columnList = "status_label"),
                @Index(name = "idx_payment_pg_tx", columnList = "pg_transaction_id"),
                @Index(name = "idx_payment_paid_at", columnList = "paid_at")
        }
)
public class Payment extends BaseAuditEntity {

    /**
     * 환불 기본 허용 기한(일). 7일 이내 환불 가능.
     */
    public static final int DEFAULT_REFUND_DAYS = 7;

    @Column(name = "request_id", length = 50, nullable = false, updatable = false)
    private String requestId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "KRW";

    @Enumerated(EnumType.STRING)
    @Column(name = "method_type", length = 30, nullable = false)
    private MethodTypeColumn methodType;

    @Column(name = "method_display", length = 200, nullable = false)
    private String methodDisplay;

    @Column(name = "method_meta", columnDefinition = "TEXT")
    private String methodMeta;

    @Column(name = "status_label", length = 30, nullable = false)
    private String statusLabel = "PENDING";

    @Column(name = "status_changed_at", nullable = false)
    private LocalDateTime statusChangedAt = LocalDateTime.now();

    @Column(name = "pg_transaction_id", length = 100)
    private String pgTransactionId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "refunded_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(name = "last_refund_at")
    private LocalDateTime lastRefundAt;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * JPA를 위한 method 타입 칼럼 enum.
     */
    public enum MethodTypeColumn {
        CREDIT_CARD,
        BANK_TRANSFER,
        MOBILE_PAYMENT,
        VIRTUAL_ACCOUNT
    }

    protected Payment() {
        super();
    }

    private Payment(Long orderId, Long userId, BigDecimal amount, String currency, PaymentMethod method) {
        super();
        this.requestId = UUID.randomUUID().toString();
        this.orderId = Objects.requireNonNull(orderId, "orderId 필수");
        this.userId = Objects.requireNonNull(userId, "userId 필수");
        Objects.requireNonNull(amount, "결제 금액은 null일 수 없습니다");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("결제 금액은 양수여야 합니다: " + amount);
        }
        this.amount = amount;
        this.currency = Objects.requireNonNullElse(currency, "KRW");
        Objects.requireNonNull(method, "결제 수단은 null일 수 없습니다");
        applyMethod(method);
        this.statusLabel = "PENDING";
        this.statusChangedAt = LocalDateTime.now();
    }

    /**
     * 결제 요청 생성.
     */
    public static Payment request(Long orderId, Long userId, BigDecimal amount, String currency, PaymentMethod method) {
        return new Payment(orderId, userId, amount, currency, method);
    }

    private void applyMethod(PaymentMethod method) {
        this.methodDisplay = method.getDisplayName();
        this.methodType = MethodTypeColumn.valueOf(method.typeCode());
        this.methodMeta = switch (method) {
            case PaymentMethod.CreditCard c ->
                    "cardCompany=%s; installment=%d; isCredit=%s"
                            .formatted(c.cardCompany(), c.installmentMonths(), c.isCredit());
            case PaymentMethod.BankTransfer b -> "bank=%s".formatted(b.bankName());
            case PaymentMethod.MobilePayment m -> "provider=%s".formatted(m.provider());
            case PaymentMethod.VirtualAccount v ->
                    "bank=%s; expiresAt=%s".formatted(v.bankName(), v.expiresAt());
        };
    }

    public String getRequestId() {
        return requestId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public MethodTypeColumn getMethodType() {
        return methodType;
    }

    public String getMethodDisplay() {
        return methodDisplay;
    }

    public String getMethodMeta() {
        return methodMeta;
    }

    public String getPgTransactionId() {
        return pgTransactionId;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public LocalDateTime getLastRefundAt() {
        return lastRefundAt;
    }

    public String getMetadata() {
        return metadata;
    }

    /**
     * 현재 상태를 sealed record 로 복원하여 반환한다.
     */
    public PaymentStatus getStatus() {
        return switch (statusLabel) {
            case "PENDING" -> new PaymentStatus.Pending(statusChangedAt);
            case "COMPLETED" -> new PaymentStatus.Completed(paidAt, pgTransactionId);
            case "FAILED" -> new PaymentStatus.Failed(failureReason, statusChangedAt);
            case "CANCELLED" -> new PaymentStatus.Cancelled(cancelReason, statusChangedAt);
            case "PARTIALLY_REFUNDED" -> new PaymentStatus.PartiallyRefunded(refundedAmount, lastRefundAt);
            case "FULLY_REFUNDED" -> new PaymentStatus.FullyRefunded(lastRefundAt);
            default -> throw new IllegalStateException("알 수 없는 결제 상태: " + statusLabel);
        };
    }

    /**
     * 결제 완료 처리. PG로부터 트랜잭션 ID를 받은 시점에 호출된다.
     */
    public void complete(String pgTransactionId, PaymentMethod method) {
        if (!"PENDING".equals(statusLabel)) {
            throw new IllegalStateException("대기 상태가 아니므로 완료 처리 불가: " + statusLabel);
        }
        Objects.requireNonNull(pgTransactionId, "PG 트랜잭션 ID는 null일 수 없습니다");
        if (pgTransactionId.isBlank()) {
            throw new IllegalArgumentException("PG 트랜잭션 ID가 비어있습니다");
        }
        this.pgTransactionId = pgTransactionId;
        this.paidAt = LocalDateTime.now();
        this.statusLabel = "COMPLETED";
        this.statusChangedAt = this.paidAt;
        registerEvent(PaymentEvents.PaymentCompletedEvent.of(getId(), orderId, userId, amount, method));
    }

    /**
     * 결제 실패 처리.
     */
    public void fail(String reason) {
        if (!"PENDING".equals(statusLabel)) {
            throw new IllegalStateException("대기 상태가 아니므로 실패 처리 불가: " + statusLabel);
        }
        this.failureReason = Objects.requireNonNullElse(reason, "결제 실패");
        this.statusLabel = "FAILED";
        this.statusChangedAt = LocalDateTime.now();
        registerEvent(PaymentEvents.PaymentFailedEvent.of(getId(), orderId, userId, this.failureReason));
    }

    /**
     * 결제 취소 처리. 결제 완료 전에만 가능.
     */
    public void cancel(String reason) {
        if (!"PENDING".equals(statusLabel)) {
            throw new IllegalStateException("결제 완료 후에는 cancel 대신 환불을 사용해야 합니다");
        }
        this.cancelReason = Objects.requireNonNullElse(reason, "사용자 취소");
        this.statusLabel = "CANCELLED";
        this.statusChangedAt = LocalDateTime.now();
    }

    /**
     * 부분/전체 환불 적용.
     */
    public void applyRefund(BigDecimal refundAmount) {
        Objects.requireNonNull(refundAmount, "환불 금액은 null일 수 없습니다");
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("환불 금액은 양수여야 합니다: " + refundAmount);
        }
        if (!isRefundable()) {
            throw new IllegalStateException("환불 불가 상태: " + statusLabel);
        }
        BigDecimal totalRefunded = refundedAmount.add(refundAmount);
        if (totalRefunded.compareTo(amount) > 0) {
            throw new IllegalStateException(
                    "환불 누계 %s 가 결제 금액 %s 를 초과합니다".formatted(totalRefunded, amount));
        }
        this.refundedAmount = totalRefunded;
        this.lastRefundAt = LocalDateTime.now();
        if (totalRefunded.compareTo(amount) == 0) {
            this.statusLabel = "FULLY_REFUNDED";
        } else {
            this.statusLabel = "PARTIALLY_REFUNDED";
        }
        this.statusChangedAt = this.lastRefundAt;
        registerEvent(PaymentEvents.RefundCompletedEvent.of(getId(), orderId, refundAmount));
    }

    /**
     * 결제 완료 상태인지 확인.
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(statusLabel)
                || "PARTIALLY_REFUNDED".equals(statusLabel);
    }

    /**
     * 환불 가능 여부.
     */
    public boolean isRefundable() {
        if (!isCompleted()) return false;
        if (paidAt == null) return false;
        return paidAt.plusDays(DEFAULT_REFUND_DAYS).isAfter(LocalDateTime.now());
    }

    /**
     * 잔여 환불 가능 금액.
     */
    public BigDecimal getRemainingRefundableAmount() {
        return amount.subtract(refundedAmount).max(BigDecimal.ZERO);
    }

    public void updateMetadata(String metadata) {
        this.metadata = metadata;
    }

    /**
     * 디버깅용 요약.
     */
    public String summary() {
        return """
                Payment[id=%s, order=%s, user=%s, amount=%s %s, status=%s, paid=%s, refunded=%s]
                """.formatted(getId(), orderId, userId, amount, currency, statusLabel, paidAt, refundedAmount);
    }
}
