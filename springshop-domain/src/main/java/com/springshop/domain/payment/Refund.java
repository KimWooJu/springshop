package com.springshop.domain.payment;

import com.springshop.domain.base.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 환불 엔티티.
 *
 * <p>{@link Payment}로부터 파생되어, 1건의 환불 요청과 처리 결과를 기록한다. 부분 환불을
 * 지원하므로 한 결제에 다수의 Refund가 연결될 수 있다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "refunds",
        indexes = {
                @Index(name = "idx_refund_payment", columnList = "payment_id"),
                @Index(name = "idx_refund_order", columnList = "order_id"),
                @Index(name = "idx_refund_status", columnList = "status")
        }
)
public class Refund extends BaseAuditEntity {

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "reason", length = 500, nullable = false)
    private String reason;

    @Column(name = "refund_method", length = 30)
    private String refundMethod;

    @Column(name = "pg_refund_id", length = 100)
    private String pgRefundId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "memo", length = 1000)
    private String memo;

    /**
     * 환불 처리 상태.
     */
    public enum Status {
        /** 환불 요청 등록 완료, PG 처리 대기. */
        PENDING,
        /** PG에 요청 전달됨, 응답 대기. */
        PROCESSING,
        /** 환불 처리 성공. */
        COMPLETED,
        /** 환불 처리 실패. */
        FAILED
    }

    protected Refund() {
        super();
    }

    private Refund(Long paymentId, Long orderId, BigDecimal amount, String reason, String refundMethod) {
        super();
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId 필수");
        this.orderId = Objects.requireNonNull(orderId, "orderId 필수");
        Objects.requireNonNull(amount, "환불 금액 필수");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("환불 금액은 양수여야 합니다: " + amount);
        }
        this.amount = amount;
        this.reason = Objects.requireNonNullElse(reason, "환불 요청");
        this.refundMethod = refundMethod;
        this.status = Status.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    /**
     * 환불 요청 생성.
     */
    public static Refund request(Long paymentId, Long orderId, BigDecimal amount, String reason, String refundMethod) {
        return new Refund(paymentId, orderId, amount, reason, refundMethod);
    }

    /**
     * PG 처리 시작 (PG 호출 직전).
     */
    public void startProcessing() {
        if (status != Status.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 처리 시작 가능: " + status);
        }
        this.status = Status.PROCESSING;
    }

    /**
     * 환불 완료 처리. PG 환불 ID를 받아 저장한다.
     */
    public void complete(String pgRefundId) {
        if (status != Status.PROCESSING && status != Status.PENDING) {
            throw new IllegalStateException("완료 처리 불가 상태: " + status);
        }
        this.pgRefundId = Objects.requireNonNull(pgRefundId, "PG 환불 ID는 null일 수 없습니다");
        this.status = Status.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 환불 실패 처리.
     */
    public void fail(String failureReason) {
        if (status == Status.COMPLETED) {
            throw new IllegalStateException("이미 완료된 환불은 실패 처리 불가");
        }
        this.failureReason = Objects.requireNonNullElse(failureReason, "환불 실패");
        this.status = Status.FAILED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 메모 추가.
     */
    public void appendMemo(String memo) {
        if (memo == null || memo.isBlank()) {
            return;
        }
        this.memo = (this.memo == null || this.memo.isBlank())
                ? memo
                : this.memo + "\n---\n" + memo;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public String getRefundMethod() {
        return refundMethod;
    }

    public String getPgRefundId() {
        return pgRefundId;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getMemo() {
        return memo;
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    public boolean isFinal() {
        return status == Status.COMPLETED || status == Status.FAILED;
    }

    @Override
    public String toString() {
        return "Refund[id=%s, paymentId=%s, amount=%s, status=%s]"
                .formatted(getId(), paymentId, amount, status);
    }
}
