package com.springshop.domain.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 결제 상태를 sealed interface로 표현한다.
 *
 * <p>상태 흐름:
 * <pre>
 * Pending ─→ Completed ─→ PartiallyRefunded ─→ FullyRefunded
 *    │           │                              ↑
 *    ↓           └─────────────────────────────┘
 * Failed     Cancelled
 * </pre>
 *
 * @author SpringShop Domain Team
 */
public sealed interface PaymentStatus
        permits PaymentStatus.Pending,
                PaymentStatus.Completed,
                PaymentStatus.Failed,
                PaymentStatus.Cancelled,
                PaymentStatus.PartiallyRefunded,
                PaymentStatus.FullyRefunded {

    /**
     * 결제 요청 직후의 대기 상태.
     */
    record Pending(LocalDateTime requestedAt) implements PaymentStatus {
        public Pending {
            if (requestedAt == null) {
                requestedAt = LocalDateTime.now();
            }
        }
    }

    /**
     * 결제 완료. PG 트랜잭션 ID가 발급된 상태.
     */
    record Completed(
            LocalDateTime paidAt,
            String pgTransactionId
    ) implements PaymentStatus {

        public Completed {
            if (paidAt == null) {
                paidAt = LocalDateTime.now();
            }
            Objects.requireNonNull(pgTransactionId, "PG 트랜잭션 ID는 null일 수 없습니다");
            if (pgTransactionId.isBlank()) {
                throw new IllegalArgumentException("PG 트랜잭션 ID가 비어있습니다");
            }
        }

        /**
         * 환불 가능 여부 판단. daysLimit 일 이내라면 가능.
         */
        public boolean isRefundable(int daysLimit) {
            if (daysLimit < 0) {
                throw new IllegalArgumentException("환불 기한은 0 이상이어야 합니다: " + daysLimit);
            }
            return paidAt.plusDays(daysLimit).isAfter(LocalDateTime.now());
        }
    }

    /**
     * 결제 실패.
     */
    record Failed(
            String reason,
            LocalDateTime failedAt
    ) implements PaymentStatus {

        public Failed {
            if (failedAt == null) {
                failedAt = LocalDateTime.now();
            }
            if (reason == null || reason.isBlank()) {
                reason = "결제 실패 (사유 미상)";
            }
        }
    }

    /**
     * 결제 취소 (완료 전 사용자/시스템에 의해 중단).
     */
    record Cancelled(
            String reason,
            LocalDateTime cancelledAt
    ) implements PaymentStatus {

        public Cancelled {
            if (cancelledAt == null) {
                cancelledAt = LocalDateTime.now();
            }
            if (reason == null) {
                reason = "사용자 취소";
            }
        }
    }

    /**
     * 부분 환불. 환불 가능 금액이 남아있다.
     */
    record PartiallyRefunded(
            BigDecimal refundedAmount,
            LocalDateTime lastRefundAt
    ) implements PaymentStatus {

        public PartiallyRefunded {
            Objects.requireNonNull(refundedAmount, "환불 금액은 null일 수 없습니다");
            if (refundedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("환불 금액은 양수여야 합니다: " + refundedAmount);
            }
            if (lastRefundAt == null) {
                lastRefundAt = LocalDateTime.now();
            }
        }
    }

    /**
     * 전액 환불 완료.
     */
    record FullyRefunded(LocalDateTime refundedAt) implements PaymentStatus {
        public FullyRefunded {
            if (refundedAt == null) {
                refundedAt = LocalDateTime.now();
            }
        }
    }

    /**
     * 최종 상태인지 판단. (더 이상 전이되지 않음)
     */
    default boolean isFinal() {
        return switch (this) {
            case Completed c -> false;
            case PartiallyRefunded p -> false;
            case Pending p -> false;
            case Failed f -> true;
            case Cancelled c -> true;
            case FullyRefunded r -> true;
        };
    }

    /**
     * 성공으로 분류되는 상태인지 판단.
     */
    default boolean isSuccess() {
        return this instanceof Completed || this instanceof PartiallyRefunded;
    }

    /**
     * 상태 라벨(영문 코드).
     */
    default String label() {
        return switch (this) {
            case Pending p -> "PENDING";
            case Completed c -> "COMPLETED";
            case Failed f -> "FAILED";
            case Cancelled c -> "CANCELLED";
            case PartiallyRefunded p -> "PARTIALLY_REFUNDED";
            case FullyRefunded r -> "FULLY_REFUNDED";
        };
    }
}
