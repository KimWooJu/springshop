package com.springshop.domain.payment;

import com.springshop.domain.base.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 결제/환불 도메인 이벤트 모음.
 *
 * <p>{@link DomainEvent} sealed permits 에 등재된 이벤트는 직접 DomainEvent를 구현하며,
 * 그렇지 않은 이벤트(RefundRequestedEvent)는 별도 record 로 정의되어 GenericEvent 로
 * 변환 후 등록된다.</p>
 *
 * @author SpringShop Domain Team
 */
public final class PaymentEvents {

    private PaymentEvents() {
        throw new UnsupportedOperationException("인스턴스화 금지");
    }

    /**
     * 결제 완료 이벤트.
     */
    public record PaymentCompletedEvent(
            UUID eventId,
            Instant occurredAt,
            Long aggregateId,
            Long orderId,
            Long userId,
            BigDecimal amount,
            String paymentMethodTypeCode,
            LocalDateTime paidAt
    ) implements DomainEvent {

        public PaymentCompletedEvent {
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
            Objects.requireNonNull(orderId, "orderId");
            Objects.requireNonNull(userId, "userId");
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(paymentMethodTypeCode, "paymentMethodTypeCode");
            if (paidAt == null) paidAt = LocalDateTime.now();
        }

        public static PaymentCompletedEvent of(Long paymentId,
                                               Long orderId,
                                               Long userId,
                                               BigDecimal amount,
                                               PaymentMethod method) {
            return new PaymentCompletedEvent(
                    UUID.randomUUID(),
                    Instant.now(),
                    paymentId,
                    orderId,
                    userId,
                    amount,
                    method.typeCode(),
                    LocalDateTime.now()
            );
        }
    }

    /**
     * 결제 실패 이벤트.
     */
    public record PaymentFailedEvent(
            UUID eventId,
            Instant occurredAt,
            Long aggregateId,
            Long orderId,
            Long userId,
            String reason,
            LocalDateTime failedAt
    ) implements DomainEvent {

        public PaymentFailedEvent {
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
            Objects.requireNonNull(orderId, "orderId");
            Objects.requireNonNull(userId, "userId");
            if (reason == null || reason.isBlank()) reason = "결제 실패";
            if (failedAt == null) failedAt = LocalDateTime.now();
        }

        public static PaymentFailedEvent of(Long paymentId,
                                            Long orderId,
                                            Long userId,
                                            String reason) {
            return new PaymentFailedEvent(
                    UUID.randomUUID(),
                    Instant.now(),
                    paymentId,
                    orderId,
                    userId,
                    reason,
                    LocalDateTime.now()
            );
        }
    }

    /**
     * 환불 완료 이벤트.
     */
    public record RefundCompletedEvent(
            UUID eventId,
            Instant occurredAt,
            Long aggregateId,
            Long orderId,
            BigDecimal refundedAmount,
            LocalDateTime completedAt
    ) implements DomainEvent {

        public RefundCompletedEvent {
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
            Objects.requireNonNull(orderId, "orderId");
            Objects.requireNonNull(refundedAmount, "refundedAmount");
            if (completedAt == null) completedAt = LocalDateTime.now();
        }

        public static RefundCompletedEvent of(Long paymentId,
                                              Long orderId,
                                              BigDecimal amount) {
            return new RefundCompletedEvent(
                    UUID.randomUUID(),
                    Instant.now(),
                    paymentId,
                    orderId,
                    amount,
                    LocalDateTime.now()
            );
        }
    }

    /**
     * 환불 요청 이벤트. DomainEvent sealed permits 에는 등록되어 있지 않으므로
     * 직접 영속화/패턴매칭 대상은 아니다. 등록 시에는
     * {@link DomainEvent.GenericEvent} 로 변환되어 publish 된다.
     */
    public record RefundRequestedEvent(
            UUID eventId,
            Instant occurredAt,
            Long paymentId,
            Long orderId,
            BigDecimal refundAmount,
            String reason
    ) {

        public RefundRequestedEvent {
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
            Objects.requireNonNull(paymentId, "paymentId");
            Objects.requireNonNull(orderId, "orderId");
            Objects.requireNonNull(refundAmount, "refundAmount");
            if (reason == null || reason.isBlank()) reason = "환불 요청";
        }

        public static RefundRequestedEvent of(Long paymentId, Long orderId, BigDecimal amount, String reason) {
            return new RefundRequestedEvent(UUID.randomUUID(), Instant.now(), paymentId, orderId, amount, reason);
        }

        /**
         * GenericEvent 로 변환하여 도메인 이벤트 버스에 실어 보낸다.
         */
        public DomainEvent.GenericEvent toGenericEvent() {
            return new DomainEvent.GenericEvent(
                    eventId,
                    occurredAt,
                    paymentId,
                    "RefundRequested: orderId=%s, amount=%s, reason=%s"
                            .formatted(orderId, refundAmount, reason)
            );
        }
    }
}
